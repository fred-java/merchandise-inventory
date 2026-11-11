package com.mateus.merchandiseinventory.dao;

import com.mateus.merchandiseinventory.exception.InvalidSortPropertyException;
import org.springframework.data.domain.Sort;

import java.util.Arrays;
import java.util.StringJoiner;
import java.util.function.Function;

public enum ProductSortField {

    ID("id", "id"),
    NAME("name", "name"),
    PRICE("price", "price"),
    STOCK_QUANTITY("stockQuantity", "stock_quantity"),
    CREATED_AT("createdAt", "created_at"),
    UPDATED_AT("updatedAt", "updated_at");

    private final String property;
    private final String column;

    ProductSortField(String property, String column) {
        this.property = property;
        this.column = column;
    }

    public String property() {
        return property;
    }

    public String column() {
        return column;
    }

    public static ProductSortField fromProperty(String property) {
        return Arrays.stream(values())
                .filter(field -> field.property.equals(property))
                .findFirst()
                .orElseThrow(() -> new InvalidSortPropertyException(property,
                        Arrays.stream(values()).map(ProductSortField::property).toList()));
    }

    public static String orderByClause(Sort sort, Function<ProductSortField, String> expression) {
        StringJoiner clause = new StringJoiner(", ");

        for (Sort.Order order : sort) {
            ProductSortField field = fromProperty(order.getProperty());
            clause.add(expression.apply(field) + " " + order.getDirection().name());
        }

        if (sort.getOrderFor(ID.property) == null) {
            clause.add(expression.apply(ID) + " ASC");
        }
        return clause.toString();
    }
}
