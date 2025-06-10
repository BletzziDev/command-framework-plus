/*
 * Copyright 2020 Luiz Carlos Mourão Paes de Carvalho
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package me.saiintbrisson.minecraft.command.argument;

import java.util.HashMap;

/**
 * The AdapterMap contains adapters such as
 * primitive values and such.
 *
 * <p>It can be created with the default primitive types
 * or totally empty, after other values can be added</p>
 *
 * @author Luiz Carlos Mourão
 */
public class AdapterMap extends HashMap<Class<?>, TypeAdapter<?>> {

    /**
     * Creates a new AdapterMap that can be empty
     * or registered with the default values.
     *
     * @param registerDefault Boolean
     */
    public AdapterMap(boolean registerDefault) {
        super();
        if (!registerDefault) return;

        put(String.class, String::valueOf);
        put(Character.class, argument -> argument.charAt(0));
        put(Integer.class, Integer::valueOf);

        /*
          Old adapter: put(Double.class, Double::valueOf);
          This new adapter prevent user malicious NaN and Infinite values input;
         */
        put(Double.class, (string) -> {
            final double value = Double.parseDouble(string);
            if(Double.isNaN(value) || Double.isInfinite(value)) return null;
            return value;
        });

        /*
         * Old adapter: put(Float.class, Float::valueOf);
         * This new adapter prevent user malicious NaN and Infinite values input;
         */
        put(Float.class, (string) -> {
            final float value = Float.parseFloat(string);
            if(Float.isNaN(value) || Float.isInfinite(value)) return null;
            return value;
        });

        put(Long.class, Long::valueOf);
        put(Boolean.class, Boolean::valueOf);
        put(Byte.class, Byte::valueOf);

        put(Character.TYPE, argument -> argument.charAt(0));
        put(Integer.TYPE, Integer::parseInt);

        /*
         * Old adapter: put(Double.TYPE, Double::parseDouble);
         * This new adapter prevent user malicious NaN and Infinite values input;
         */
        put(Double.TYPE, (string) -> {
            final double value = Double.parseDouble(string);
            if(Double.isNaN(value) || Double.isInfinite(value)) return null;
            return value;
        });

        /*
         * Old adapter: put(Float.TYPE, Float::parseFloat);
         * This new adapter prevent user malicious NaN and Infinite values input;
         */
        put(Float.TYPE, (string) -> {
            final float value = Float.parseFloat(string);
            if(Float.isNaN(value) || Float.isInfinite(value)) return null;
            return value;
        });

        put(Long.TYPE, Long::parseLong);
        put(Boolean.TYPE, Boolean::parseBoolean);
        put(Byte.TYPE, Byte::parseByte);
    }

    @SuppressWarnings("unchecked")
    public <T> TypeAdapter<T> put(Class<T> key, TypeAdapter<T> value) {
        return (TypeAdapter<T>) super.put(key, value);
    }

}
