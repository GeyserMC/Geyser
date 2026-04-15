/*
 * Copyright (c) 2026 GeyserMC. http://geysermc.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/Geyser
 */

package org.geysermc.geyser.item.hashing;

import com.google.common.base.Suppliers;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Utility interface with utility methods for creating {@link MapBuilder}s for a scenario in which there is an abstract type {@code A} ({@link ValueType}), indicated by a {@code typeKey} of {@link DistinctType},
 * and for each implementation of {@code A}, a {@link MapBuilder} exists, hashing an instance of the implementation.
 * This interface builds on the {@link MapBuilder#dispatch(String, MinecraftHasher, Function, Function)} method to achieve this.
 *
 * <p>Essentially, implementations of this interface, when grouped together in e.g. an enum, function as a fancy named map from instances of {@code A} to their respective hashers.</p>
 *
 * <p>A common way of implementing this is as follows:</p>
 *
 * <ol>
 *     <li>Create an enum, implementing {@link EnumMapDispatchHasher}, with the enum as {@link DistinctType} and the base type {@code A} as {@link ValueType}.</li>
 *     <li>Add a {@code clazz} field to the enum, of type {@code Class<? extends A>}, and a {@code mapBuilder} field, of type {@code MapBuilder<? extends A>}.
 *     <ul>
 *         <li>The {@code clazz} field is used to recognise an implementation of {@code A} and find an instance of {@link EnumMapDispatchHasher} for a certain instance of an implementation of {@code A}.
 *
 *         <p>It is therefore important that the <em>{@code clazz} field is unique for all constants in the enum</em>.</p>
 *         </li>
 *         <li>The {@code mapBuilder} field is then used to hash that implementation of {@code A}.</li>
 *         <li>You can use a typed constructor like this to ensure that the type of {@code clazz} is the same as {@code mapBuilder}, and to provide proper typing information for the {@code mapBuilder}:
 *
 *         <p>{@code <T extends A> MyExampleEnum(Class<T> clazz, MapBuilder<T> builder)}</p>
 *
 *         <p>In which {@code T} is an implementation of {@code A}, and {@code A} is the base type to hash.</p>
 *         </li>
 *     </ul>
 *     </li>
 *     <li>For each implementation of {@code A}, add a constant to that enum, specifying the {@code clazz} of that implementation, and a {@code mapBuilder} for it.
 *
 *     <p><em>The name of the constant will be used to encode the value of the {@code typeKey}, so be sure it is right and matches Java edition!</em></p>
 *     </li>
 *     <li>The methods of {@link EnumMapDispatchHasher} are to be implemented as follows:
 *     <ul>
 *         <li>{@link EnumMapDispatchHasher#distinction()} returns {@code this}.</li>
 *         <li>{@link EnumMapDispatchHasher#valueTypeClass()} returns the {@code clazz} field.</li>
 *         <li>{@link EnumMapDispatchHasher#mapBuilder()} returns the {@code mapBuilder} field.</li>
 *     </ul>
 *     </li>
 *     <li>Finally, create the {@link MapBuilder} with the {@link EnumMapDispatchHasher#dispatch(Supplier)} or the {@link EnumMapDispatchHasher#dispatch(String, Supplier)} method,
 *     using {@code MyExampleEnum::values} as {@code hashersSupplier}.</li>
 * </ol>
 *
 * <p>Example implementations can be seen in {@link org.geysermc.geyser.item.hashing.data.ConsumeEffectType}, {@link org.geysermc.geyser.item.hashing.data.NbtComponentType}, and {@link org.geysermc.geyser.item.hashing.data.ObjectContentsType}.
 * If a {@link MinecraftHasher} is required instead of a {@link MapBuilder}, use {@link MinecraftHasher#mapBuilder(MapBuilder)} to wrap the {@link MapBuilder} into a hasher.</p>
 *
 * <p>The implementation described above uses the enum itself as {@link DistinctType} for {@code A}, and then uses {@link EnumMapDispatchHasher#dispatch(String, Supplier)}, which uses {@link MinecraftHasher#fromEnum()} to
 * encode the enum constant for {@code typeKey}. This may not always be wanted behaviour, and this interface does allow using a different {@link DistinctType}. In that case, the hasher for the {@link DistinctType} has
 * to be given, using the {@link EnumMapDispatchHasher#dispatch(MinecraftHasher, Supplier)} or the {@link EnumMapDispatchHasher#dispatch(String, MinecraftHasher, Supplier)} method.</p>
 *
 * @param <DistinctType> the distinct type used to indicate the implementation of a {@link ValueType}.
 * @param <ValueType> the abstract, base type to hash.
 * @see EnumMapDispatchHasher#dispatch(Supplier)
 * @see EnumMapDispatchHasher#dispatch(String, Supplier)
 * @see EnumMapDispatchHasher#dispatch(MinecraftHasher, Supplier)
 * @see EnumMapDispatchHasher#dispatch(String, MinecraftHasher, Supplier)
 */
public interface EnumMapDispatchHasher<DistinctType, ValueType> {

    /**
     * Returns the {@link DistinctType} of this implementation of {@link ValueType}.
     *
     * <p><em>Implementations must ensure that each {@link DistinctType} is unique and only for one implementation of {@link ValueType}.</em></p>
     *
     * @return the {@link DistinctType} of this implementation of {@link ValueType}.
     */
    DistinctType distinction();

    /**
     * @return the {@link Class} of this implementation of {@link ValueType}.
     */
    Class<? extends ValueType> valueTypeClass();

    /**
     * @return the {@link MapBuilder} used to encode this implementation of {@link ValueType}.
     */
    MapBuilder<? extends ValueType> mapBuilder();

    /**
     * Creates a {@link MapBuilder} for an abstract type {@link Value}, using an array of implementations of {@link EnumMapDispatchHasher}. This {@link MapBuilder} encodes the
     * {@link Value} fuzzily, as in, there is no {@code type} key indicating the {@code DistinctType}. As such, implementing a {@code DistinctType} to indicate the type is not strictly necessary.
     *
     * @param hashersSupplier a supplier returning the array of {@link EnumMapDispatchHasher} implementations, one for each implementation of {@link Value}.
     * @param <Value> the abstract, base type to create a {@link MapBuilder} for.
     * @return the created {@link MapBuilder}.
     */
    static <Value, HasherEnum extends EnumMapDispatchHasher<?, Value>> MapBuilder<Value> dispatchFuzzy(Supplier<HasherEnum[]> hashersSupplier) {
        Supplier<HasherEnum[]> memoizedHashers = Suppliers.memoize(hashersSupplier::get);
        return builder -> builder.accept(Function.identity(), value -> {
            HasherEnum[] hashers = memoizedHashers.get();
            for (HasherEnum hasher : hashers) {
                if (hasher.valueTypeClass().isInstance(value)) {
                    return (MapBuilder<Value>) hasher.mapBuilder();
                }
            }
            throw new IllegalArgumentException("No hasher for value " + value);
        });
    }

    /**
     * Delegates to {@link EnumMapDispatchHasher#dispatch(String, Supplier)}, uses {@code "type"} as {@code typeKey}.
     *
     * @see EnumMapDispatchHasher#dispatch(String, Supplier)
     * @see EnumMapDispatchHasher#dispatch(String, MinecraftHasher, Supplier)
     */
    static <Value, HasherEnum extends Enum<HasherEnum> & EnumMapDispatchHasher<HasherEnum, Value>> MapBuilder<Value> dispatch(Supplier<HasherEnum[]> hashersSupplier) {
        return dispatch("type", hashersSupplier);
    }

    /**
     * Delegates to {@link EnumMapDispatchHasher#dispatch(String, MinecraftHasher, Supplier)}, using the {@link HasherEnum} as {@code Distinct} type.
     *
     * <p>Uses {@link MinecraftHasher#fromEnum()} as hasher for {@link HasherEnum}/the {@code Distinct} type, so be sure that the enum constants match what you want to appear in the map!</p>
     *
     * @see EnumMapDispatchHasher#dispatch(String, MinecraftHasher, Supplier)
     */
    static <Value, HasherEnum extends Enum<HasherEnum> & EnumMapDispatchHasher<HasherEnum, Value>> MapBuilder<Value> dispatch(String typeKey, Supplier<HasherEnum[]> hashersSupplier) {
        return dispatch(typeKey, MinecraftHasher.fromEnum(), hashersSupplier);
    }

    /**
     * Delegates to {@link EnumMapDispatchHasher#dispatch(String, MinecraftHasher, Supplier)}, using {@code "type"} as {@code distinctTypeKey}.
     *
     * @see EnumMapDispatchHasher#dispatch(String, MinecraftHasher, Supplier)
     */
    static <Distinct, Value, HasherEnum extends EnumMapDispatchHasher<Distinct, Value>> MapBuilder<Value> dispatch(MinecraftHasher<Distinct> distinctHasher, Supplier<HasherEnum[]> hashersSupplier) {
        return dispatch("type", distinctHasher, hashersSupplier);
    }

    /**
     * Creates a {@link MapBuilder} for an abstract type {@link Value}, using an array of implementations of {@link EnumMapDispatchHasher}, and the {@code distinctTypeKey} to indicate the {@link Distinct} type of
     * an implementation of {@link Value}.
     *
     * @param distinctTypeKey the key in the map used to indicate the {@link Distinct} type.
     * @param distinctHasher the hasher used to hash the {@link Distinct} type.
     * @param hashersSupplier a supplier returning the array of {@link EnumMapDispatchHasher} implementations, one for each implementation of {@link Value}.
     * @param <Distinct> the distinct type used to indicate an implementation of {@link Value}.
     * @param <Value> the abstract, base type to create a {@link MapBuilder} for.
     * @return the created {@link MapBuilder}.
     */
    static <Distinct, Value, HasherEnum extends EnumMapDispatchHasher<Distinct, Value>> MapBuilder<Value> dispatch(String distinctTypeKey, MinecraftHasher<Distinct> distinctHasher,
                                                                                                                   Supplier<HasherEnum[]> hashersSupplier) {
        Supplier<HasherEnum[]> memoizedHashers = Suppliers.memoize(hashersSupplier::get);
        MinecraftHasher<HasherEnum> enumDistinctHasher = distinctHasher.cast(EnumMapDispatchHasher::distinction);
        return MapBuilder.dispatch(distinctTypeKey, enumDistinctHasher, value -> {
            HasherEnum[] hashers = memoizedHashers.get();
            for (HasherEnum hasher : hashers) {
                if (hasher.valueTypeClass().isInstance(value)) {
                    return hasher;
                }
            }
            throw new IllegalArgumentException("No hasher for value " + value);
        }, hasher -> hasher.mapBuilder().cast());
    }
}
