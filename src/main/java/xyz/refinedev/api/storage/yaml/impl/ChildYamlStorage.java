package xyz.refinedev.api.storage.yaml.impl;

import java.lang.reflect.Field;
import java.util.List;

public abstract class ChildYamlStorage {

    /**
     * The parent storage of this child storage
     */
    private final ParentYamlStorage parentStorage;

    /**
     * Create an instance of a Child Yaml Storage with a parent storage
     *
     * @param parentStorage {@link ParentYamlStorage parent}
     */
    public ChildYamlStorage(ParentYamlStorage parentStorage) {
        this.parentStorage = parentStorage;
    }

    public abstract List<Field> getConfigFields();

    /**
     * Returns the parent storage of this storage
     *
     * @return {@link ParentYamlStorage storage}
     */
    public ParentYamlStorage getParentStorage() {
        return parentStorage;
    }
}
