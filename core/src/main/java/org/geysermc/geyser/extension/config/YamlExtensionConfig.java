/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.extension.config;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.api.extension.ExtensionConfig;
import org.geysermc.geyser.api.extension.ExtensionLogger;
import org.geysermc.geyser.text.GeyserLocale;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class YamlExtensionConfig implements ExtensionConfig {
    private ConfigSection config;
    private Path file;
    private boolean correct = false;
    private ExtensionLogger logger;

    public YamlExtensionConfig(@NonNull Path file, @NonNull ExtensionLogger logger) {
        this.logger = logger;
        this.load(file, new ConfigSection());
    }

    public YamlExtensionConfig(@NonNull String file, @NonNull ExtensionLogger logger) {
        this.logger = logger;
        this.load(Path.of(file), new ConfigSection());
    }

    private void load(@NonNull Path file, @NonNull ConfigSection defaults) {
        this.correct = true;
        this.file = file;

        if (!Files.exists(file)) {
            try {
                Files.createDirectory(this.file.getParent());
                Files.createFile(this.file);
            } catch (IOException e) {
                logger.error(GeyserLocale.getLocaleStringLog("geyser.extensions.config.failed_create", this.file.toAbsolutePath().toString()), e);
            }

            this.config = defaults;
            this.save();
        } else {
            if (this.correct) {
                String path;
                try {
                    path = Files.readString(this.file);
                } catch (IOException e) {
                    logger.error(GeyserLocale.getLocaleStringLog("geyser.extensions.config.failed_read", this.file.toAbsolutePath().toString()), e);
                    return;
                }

                this.parseContent(path);

                if (!this.correct) {
                    return;
                }

                if (this.setDefault(defaults) > 0) {
                    this.save();
                }
            }
        }
    }

    @Override
    public void reload() {
        this.config.clear();
        this.correct = false;

        if (this.file == null) {
            throw new IllegalStateException("Failed to reload Config. File object is undefined.");
        }

        this.load(this.file, new ConfigSection());
    }

    @Override
    public void save() {
        if (this.file == null) {
            throw new IllegalStateException("Failed to save Config. File object is undefined.");
        }

        if (this.correct) {
            DumperOptions dumperOptions = new DumperOptions();
            dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

            Yaml yaml = new Yaml(dumperOptions);
            StringBuilder content = new StringBuilder(yaml.dump(this.config));

            try {
                Files.writeString(this.file, content.toString());
            } catch (IOException e) {
                this.logger.error(GeyserLocale.getLocaleStringLog("geyser.extensions.config.failed_save", this.file.toAbsolutePath().toString()), e);
            }
        }
    }

    @Override
    public void set(@NonNull String path, @Nullable Object value) {
        this.config.set(path, value);
    }

    @Nullable
    @Override
    public Object get(@NonNull String path) {
        return this.config.get(path);
    }

    @Override
    public int getInt(@NonNull String path) {
        return this.config.get(path, (Number) 0).intValue();
    }

    @Override
    public boolean isInt(@NonNull String path) {
        Object val = this.config.get(path);
        return val instanceof Integer;
    }

    @Override
    public long getLong(@NonNull String path) {
        return this.config.get(path, (Number) 0).longValue();
    }

    @Override
    public boolean isLong(@NonNull String path) {
        Object val = this.config.get(path);
        return val instanceof Long;
    }

    @Override
    public double getDouble(@NonNull String path) {
        return this.config.get(path, (Number) 0).doubleValue();
    }

    @Override
    public boolean isDouble(@NonNull String path) {
        Object val = this.config.get(path);
        return val instanceof Double;
    }

    @NonNull
    @Override
    public String getString(@NonNull String path) {
        Object result = this.config.get(path, "");
        return String.valueOf(result);
    }

    @Override
    public boolean isString(@NonNull String path) {
        Object val = get(path);
        return val instanceof String;
    }

    @Override
    public boolean getBoolean(@NonNull String path) {
        return this.config.get(path, false);
    }

    @Override
    public boolean isBoolean(@NonNull String path) {
        Object val = get(path);
        return val instanceof Boolean;
    }

    @Nullable
    @Override
    public List getList(@NonNull String path) {
        return this.config.get(path, null);
    }

    @Override
    public boolean isList(@NonNull String path) {
        Object val = get(path);
        return val instanceof List;
    }

    @NonNull
    @Override
    public List<String> getStringList(@NonNull String path) {
        List value = this.getList(path);

        if (value == null) {
            return new ArrayList<>(0);
        }

        List<String> result = new ArrayList<>();

        for (Object o : value) {
            if (o instanceof String || o instanceof Number || o instanceof Boolean || o instanceof Character) {
                result.add(String.valueOf(o));
            }
        }
        return result;
    }

    @NonNull
    @Override
    public List<Integer> getIntegerList(@NonNull String path) {
        List<?> list = getList(path);

        if (list == null) {
            return new ArrayList<>(0);
        }

        List<Integer> result = new ArrayList<>();

        for (Object object : list) {
            if (object instanceof Integer) {
                result.add((Integer) object);
            } else if (object instanceof String) {
                try {
                    result.add(Integer.valueOf((String) object));
                } catch (Exception ex) {
                    //ignore
                }
            } else if (object instanceof Character) {
                result.add((int) (Character) object);
            } else if (object instanceof Number) {
                result.add(((Number) object).intValue());
            }
        }
        return result;
    }

    @NonNull
    @Override
    public List<Boolean> getBooleanList(@NonNull String path) {
        List<?> list = getList(path);

        if (list == null) {
            return new ArrayList<>(0);
        }

        List<Boolean> result = new ArrayList<>();

        for (Object object : list) {
            if (object instanceof Boolean) {
                result.add((Boolean) object);
            } else if (object instanceof String) {
                if (Boolean.TRUE.toString().equals(object)) {
                    result.add(true);
                } else if (Boolean.FALSE.toString().equals(object)) {
                    result.add(false);
                }
            }
        }
        return result;
    }

    @NonNull
    @Override
    public List<Double> getDoubleList(@NonNull String path) {
        List<?> list = getList(path);

        if (list == null) {
            return new ArrayList<>(0);
        }

        List<Double> result = new ArrayList<>();

        for (Object object : list) {
            if (object instanceof Double) {
                result.add((Double) object);
            } else if (object instanceof String) {
                try {
                    result.add(Double.valueOf((String) object));
                } catch (Exception ex) {
                    //ignore
                }
            } else if (object instanceof Character) {
                result.add((double) (Character) object);
            } else if (object instanceof Number) {
                result.add(((Number) object).doubleValue());
            }
        }
        return result;
    }

    @NonNull
    @Override
    public List<Float> getFloatList(@NonNull String path) {
        List<?> list = getList(path);

        if (list == null) {
            return new ArrayList<>(0);
        }

        List<Float> result = new ArrayList<>();

        for (Object object : list) {
            if (object instanceof Float) {
                result.add((Float) object);
            } else if (object instanceof String) {
                try {
                    result.add(Float.valueOf((String) object));
                } catch (Exception ex) {
                    //ignore
                }
            } else if (object instanceof Character) {
                result.add((float) (Character) object);
            } else if (object instanceof Number) {
                result.add(((Number) object).floatValue());
            }
        }
        return result;
    }

    @NonNull
    @Override
    public List<Long> getLongList(@NonNull String path) {
        List<?> list = getList(path);

        if (list == null) {
            return new ArrayList<>(0);
        }

        List<Long> result = new ArrayList<>();

        for (Object object : list) {
            if (object instanceof Long) {
                result.add((Long) object);
            } else if (object instanceof String) {
                try {
                    result.add(Long.valueOf((String) object));
                } catch (Exception ex) {
                    //ignore
                }
            } else if (object instanceof Character) {
                result.add((long) (Character) object);
            } else if (object instanceof Number) {
                result.add(((Number) object).longValue());
            }
        }
        return result;
    }

    @NonNull
    @Override
    public List<Byte> getByteList(@NonNull String path) {
        List<?> list = getList(path);

        if (list == null) {
            return new ArrayList<>(0);
        }

        List<Byte> result = new ArrayList<>();

        for (Object object : list) {
            if (object instanceof Byte) {
                result.add((Byte) object);
            } else if (object instanceof String) {
                try {
                    result.add(Byte.valueOf((String) object));
                } catch (Exception ex) {
                    //ignore
                }
            } else if (object instanceof Character) {
                result.add((byte) ((Character) object).charValue());
            } else if (object instanceof Number) {
                result.add(((Number) object).byteValue());
            }
        }

        return result;
    }

    @NonNull
    @Override
    public List<Character> getCharacterList(@NonNull String path) {
        List<?> list = getList(path);

        if (list == null) {
            return new ArrayList<>(0);
        }

        List<Character> result = new ArrayList<>();

        for (Object object : list) {
            if (object instanceof Character) {
                result.add((Character) object);
            } else if (object instanceof String) {
                String str = (String) object;

                if (str.length() == 1) {
                    result.add(str.charAt(0));
                }
            } else if (object instanceof Number) {
                result.add((char) ((Number) object).intValue());
            }
        }

        return result;
    }

    @NonNull
    @Override
    public List<Short> getShortList(@NonNull String path) {
        List<?> list = getList(path);

        if (list == null) {
            return new ArrayList<>(0);
        }

        List<Short> result = new ArrayList<>();

        for (Object object : list) {
            if (object instanceof Short) {
                result.add((Short) object);
            } else if (object instanceof String) {
                try {
                    result.add(Short.valueOf((String) object));
                } catch (Exception ex) {
                    //ignore
                }
            } else if (object instanceof Character) {
                result.add((short) ((Character) object).charValue());
            } else if (object instanceof Number) {
                result.add(((Number) object).shortValue());
            }
        }

        return result;
    }

    @NonNull
    @Override
    public List<Map> getMapList(@NonNull String path) {
        List<Map> list = getList(path);
        List<Map> result = new ArrayList<>();

        if (list == null) {
            return result;
        }

        for (Object object : list) {
            if (object instanceof Map) {
                result.add((Map) object);
            }
        }

        return result;
    }

    @Override
    public boolean contains(@NonNull String key, boolean ignoreCase) {
        if (ignoreCase) {
            key = key.toLowerCase();
        }

        for (String existKey : this.getKeys(true)) {
            if (ignoreCase) {
                existKey = existKey.toLowerCase();
            }
            if (existKey.equals(key)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean contains(@NonNull String key) {
        return this.contains(key, false);
    }

    @Override
    public void remove(@NonNull String path) {
        this.config.remove(path);
    }

    @NonNull
    @Override
    public List<String> getKeys(boolean deep) {
        return this.config.getKeys(deep);
    }

    @NonNull
    @Override
    public List<String> getKeys() {
        return this.getKeys(true);
    }

    private int setDefault(@NonNull ConfigSection map) {
        int size = this.config.size();
        this.config = this.fillDefaults(map, this.config);
        return this.config.size() - size;
    }

    @NonNull
    private ConfigSection fillDefaults(@NonNull ConfigSection defaultMap, @NonNull ConfigSection data) {
        for (String key : defaultMap.keySet()) {
            if (!data.containsKey(key)) {
                data.put(key, defaultMap.get(key));
            }
        }
        return data;
    }

    private void parseContent(@NonNull String content) {
        DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Yaml yaml = new Yaml(dumperOptions);
        this.config = new ConfigSection(yaml.loadAs(content, LinkedHashMap.class));
    }

    private class ConfigSection extends LinkedHashMap<String, Object> {
        public ConfigSection() {
            super();
        }

        public ConfigSection(@NonNull LinkedHashMap<String, Object> map) {
            this();
            if (map.isEmpty()) {
                return;
            }

            for (Map.Entry<String, Object> entry : map.entrySet()) {
                if (entry.getValue() instanceof LinkedHashMap) {
                    super.put(entry.getKey(), new ConfigSection((LinkedHashMap) entry.getValue()));
                } else if (entry.getValue() instanceof List) {
                    super.put(entry.getKey(), parseList((List) entry.getValue()));
                } else {
                    super.put(entry.getKey(), entry.getValue());
                }
            }
        }

        @NonNull
        private List parseList(@NonNull List list) {
            List<Object> newList = new ArrayList<>();

            for (Object o : list) {
                if (o instanceof LinkedHashMap) {
                    newList.add(new ConfigSection((LinkedHashMap) o));
                } else {
                    newList.add(o);
                }
            }

            return newList;
        }

        @NonNull
        public ConfigSection getAll() {
            return new ConfigSection(this);
        }

        @Nullable
        public Object get(@NonNull String path) {
            return this.get(path, null);
        }

        @Nullable
        public <T> T get(@NonNull String path, @Nullable T defaultValue) {
            if (path.isEmpty()) {
                return defaultValue;
            }

            if (super.containsKey(path)) {
                return (T) super.get(path);
            }

            String[] keys = path.split("\\.", 2);
            if (!super.containsKey(keys[0])) {
                return defaultValue;
            }
            Object value = super.get(keys[0]);
            if (value instanceof ConfigSection) {
                ConfigSection section = (ConfigSection) value;
                return section.get(keys[1], defaultValue);
            }
            return defaultValue;
        }

        public void set(@NonNull String path, @Nullable Object value) {
            String[] subKeys = path.split("\\.", 2);
            if (subKeys.length > 1) {
                ConfigSection childSection = new ConfigSection();
                if (this.containsKey(subKeys[0]) && super.get(subKeys[0]) instanceof ConfigSection) {
                    childSection = (ConfigSection) super.get(subKeys[0]);
                }
                childSection.set(subKeys[1], value);
                super.put(subKeys[0], childSection);
            } else {
                super.put(subKeys[0], value);
            }
        }

        public void remove(@NonNull String path) {
            if (path.isEmpty()) {
                return;
            }

            if (super.containsKey(path)) {
                super.remove(path);
            } else if (this.containsKey(".")) {
                String[] keys = path.split("\\.", 2);
                if (super.get(keys[0]) instanceof ConfigSection) {
                    ConfigSection section = (ConfigSection) super.get(keys[0]);
                    section.remove(keys[1]);
                }
            }
        }

        @NonNull
        public List<String> getKeys(boolean deep) {
            List<String> keys = new LinkedList<>();
            this.forEach((key, value) -> {
                keys.add(key);
                if (value instanceof ConfigSection) {
                    if (deep) {
                        ((ConfigSection) value).getKeys(true).forEach(childKey -> keys.add(key + "." + childKey));
                    }
                }
            });
            return keys;
        }
    }
}
