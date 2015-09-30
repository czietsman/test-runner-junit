package com.redstor.qalab.junit;

import org.junit.experimental.categories.Category;
import org.junit.runner.Description;
import org.junit.runner.manipulation.Filter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This filter is based on the category filter in: org.junit.experimental.categories.Categories.CategoryFilter
 *
 * It has been adapted to allow for specifying inclusion and exclusion lists instead of only single category for
 * classes inclusion and/or exclusion
 */
class CategoryFilter extends Filter {
    private final List<Class<?>> fIncluded;

    private final List<Class<?>> fExcluded;

    public CategoryFilter(List<Class<?>> includedCategory,
                          List<Class<?>> excludedCategory) {
        fIncluded = includedCategory;
        fExcluded = excludedCategory;
    }

    @Override
    public String describe() {
        return "category";
    }

    @Override
    public boolean shouldRun(Description description) {
        if (hasCorrectCategoryAnnotation(description)) {
            return true;
        }
        for (Description each : description.getChildren()) {
            if (shouldRun(each)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasCorrectCategoryAnnotation(Description description) {
        List<Class<?>> categories = categories(description);
        if (categories.isEmpty()) {
            return fIncluded.isEmpty();
        }
        for (Class<?> each : categories) {
            for (Class<?> excluded : fExcluded) {
                if (excluded != null && excluded.isAssignableFrom(each)) {
                    return false;
                }
            }
        }
        if (fIncluded.isEmpty()) {
            return true;
        }
        for (Class<?> each : categories) {
            for (Class<?> included : fIncluded) {
                if (included.isAssignableFrom(each)) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<Class<?>> categories(Description description) {
        ArrayList<Class<?>> categories = new ArrayList<Class<?>>();
        categories.addAll(Arrays.asList(directCategories(description)));
        categories.addAll(Arrays.asList(directCategories(parentDescription(description))));
        return categories;
    }

    private Description parentDescription(Description description) {
        Class<?> testClass = description.getTestClass();
        if (testClass == null) {
            return null;
        }
        return Description.createSuiteDescription(testClass);
    }

    private Class<?>[] directCategories(Description description) {
        if (description == null) {
            return new Class<?>[0];
        }
        Category annotation = description.getAnnotation(Category.class);
        if (annotation == null) {
            return new Class<?>[0];
        }
        return annotation.value();
    }
}
