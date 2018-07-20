package icfpc2018.bot.util

import org.organicdesign.fp.collections.PersistentHashSet
import org.organicdesign.fp.collections.PersistentTreeSet

inline operator fun <T> PersistentHashSet<T>.plus(value: T): PersistentHashSet<T> = put(value)
inline operator fun <T> PersistentTreeSet<T>.plus(value: T): PersistentTreeSet<T> = put(value)
inline operator fun <T> PersistentHashSet<T>.minus(value: T): PersistentHashSet<T> = without(value)
inline operator fun <T> PersistentTreeSet<T>.minus(value: T): PersistentTreeSet<T> = without(value)

inline fun <T: Comparable<T>> persistentTreeSetOf(vararg values: T): PersistentTreeSet<T> =
        PersistentTreeSet.of(values.asList())

inline fun <T: Comparable<T>> persistentHashSetOf(vararg values: T): PersistentHashSet<T> =
        PersistentHashSet.of(values.asList())


