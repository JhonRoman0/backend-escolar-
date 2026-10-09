package com.example.Escolar.Exception;

import com.example.Escolar.Dto.ItemDeleteError;
import java.util.List;

public class BatchDeleteNotAllowedException extends RuntimeException {
    private final List<ItemDeleteError> items;

    public BatchDeleteNotAllowedException(String message, List<ItemDeleteError> items) {
        super(message);
        this.items = items;
    }

    public List<ItemDeleteError> getItems() {
        return items;
    }
}
