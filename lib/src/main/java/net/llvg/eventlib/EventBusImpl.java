package net.llvg.eventlib;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

final class EventBusImpl<E, L extends Consumer<? super E>>
  implements EventBus<E, L>
{
    private static final LoadingCache<Class<?>, ImmutableSet<Class<?>>> supertypesCache = CacheBuilder.newBuilder()
      .weakKeys()
      .build(new CacheLoader<Class<?>, ImmutableSet<Class<?>>>() {
          @Override
          public ImmutableSet<Class<?>> load(final Class<?> key)
            throws Exception {
              final ImmutableSet.Builder<Class<?>> builder = ImmutableSet.builder();
              final List<Class<?>> supers = Lists.newArrayList(
                Lists.asList(key.getSuperclass(), key.getInterfaces())
                  .stream()
                  .filter(Objects::nonNull)
                  .iterator()
              );
              builder.addAll(supers);
              for (final Class<?> it : supers) builder.addAll(supertypesCache.get(it));
              return builder.build();
          }
      });
    
    private static final LoadingCache<Method, ImmutableSet<Method>> supermethodsCache = CacheBuilder.newBuilder()
      .weakKeys()
      .build(new CacheLoader<Method, ImmutableSet<Method>>() {
          @Override public ImmutableSet<Method> load(final Method method)
            throws Exception {
              return supertypesCache.get(method.getDeclaringClass())
                .stream()
                .flatMap(it -> Arrays.stream(it.getMethods()))
                .filter(it ->
                  Objects.equals(it.getName(), method.getName()) &&
                  Arrays.equals(it.getParameterTypes(), method.getParameterTypes()) &&
                  Modifier.isStatic(it.getModifiers()) == Modifier.isStatic(method.getModifiers())
                )
                .collect(ImmutableSet.toImmutableSet());
          }
      });
    
    private final Map<Method, ListenerFactory> factories = new ConcurrentHashMap<>();
    private final FactoryLoader factoryLoader;
    
    private final Class<E> baseType;
    private final ListenerWrapper<E, ? extends L> listenerWrapper;
    private final Consumer<? super ArrayList<L>> cacheProcessor;
    private final Map<Class<? extends E>, ListenerList<E, L>> lists = new ConcurrentHashMap<>();
    
    private final Function<Class<? extends E>, ListenerList<E, L>> buildList;
    private final Function<Object, ImmutableList<L>> registerObject;
    
    private final Map<Object, ImmutableList<L>> registered = new ConcurrentHashMap<>();
    
    EventBusImpl(final EventBusBuilder<E, L> builder) {
        this.baseType = builder.getBaseType();
        this.listenerWrapper = builder.getListenerWrapper();
        this.cacheProcessor = builder.getCacheProcessor();
        
        this.factoryLoader = builder.getFactoryLoader();
        this.buildList = clazz -> new ListenerList<>(
          Lists.asList(clazz.getSuperclass(), clazz.getInterfaces())
            .stream()
            .filter(Objects::nonNull)
            .filter(baseType::isAssignableFrom)
            .map(it -> it.<E>asSubclass(baseType))
            .map(this::getList),
          cacheProcessor
        );
        this.registerObject = target -> {
            if (target instanceof Method) return buildStaticListeners(Stream.of((Method) target));
            if (target instanceof Class<?>) return buildStaticListeners(Arrays.stream(((Class<?>) target).getMethods()));
            
            return buildObjectListeners(Arrays.stream(target.getClass().getMethods()), target);
        };
    }
    
    private Optional<Class<? extends E>> getListenerType(final Method method) {
        return Optional.of(method.getParameterTypes())
          .filter(it -> it.length == 1)
          .map(it -> it[0])
          .filter(baseType::isAssignableFrom)
          .map(it -> it.asSubclass(baseType));
    }
    
    private ImmutableList<L> buildStaticListeners(final Stream<Method> methods) {
        final ImmutableList.Builder<L> builder = ImmutableList.builder();
        methods
          .filter(it -> Modifier.isStatic(it.getModifiers()))
          .map(m -> getListenerType(m)
            .flatMap(t -> listenerWrapper.wrapStaticListener(
              t,
              getFactory(m).get(null),
              m
            ).map(l -> Pair.of(t, (L) l)))
          )
          .filter(Optional::isPresent)
          .map(Optional::get)
          .peek(it -> getList(it.getKey()).add(it.getValue()))
          .map(Pair::getValue)
          .forEachOrdered(builder::add);
        return builder.build();
    }
    
    private ImmutableList<L> buildObjectListeners(final Stream<Method> methods, final Object target) {
        final ImmutableList.Builder<L> builder = ImmutableList.builder();
        methods
          .filter(it -> !Modifier.isStatic(it.getModifiers()))
          .map(m -> getListenerType(m)
            .flatMap(t -> listenerWrapper.wrapObjectListener(
              t,
              getFactory(m).get(target),
              m,
              target,
              supermethodsCache.getUnchecked(m)
            ).map(l -> Pair.of(t, l)))
          )
          .filter(Optional::isPresent)
          .map(Optional::get)
          .peek(it -> getList(it.getKey()).add(it.getValue()))
          .map(Pair::getValue)
          .forEachOrdered(builder::add);
        return builder.build();
    }
    
    private ListenerFactory getFactory(final Method method) {
        return factories.computeIfAbsent(method, factoryLoader::get);
    }
    
    @Override
    public ListenerList<E, L> getList(final Class<? extends E> clazz) {
        return lists.computeIfAbsent(clazz, buildList);
    }
    
    @Override
    public void register(final Object target) {
        registered.computeIfAbsent(target, registerObject);
    }
    
    @Override
    public void unregister(final Object target) {
        Optional.ofNullable(registered.remove(target))
          .map(it -> (Predicate<L>) it::contains)
          .ifPresent(filter -> lists.values().forEach(it -> it.removeIf(filter)));
    }
    
    @Override
    public String toString() {
        return String.format(
          "EventBus{ baseType: %s, factoryLoader: %s, listenerWrapper: %s, cacheProcessor: %s }",
          baseType,
          factoryLoader,
          listenerWrapper,
          cacheProcessor
        );
    }
}
