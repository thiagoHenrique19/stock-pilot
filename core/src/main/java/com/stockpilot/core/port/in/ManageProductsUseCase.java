package com.stockpilot.core.port.in;

import com.stockpilot.core.port.in.dto.ProductRequest;
import com.stockpilot.core.port.in.dto.ProductResponse;
import com.stockpilot.core.port.in.dto.RestockRequest;
import java.util.List;

public interface ManageProductsUseCase {

    ProductResponse register(ProductRequest request);

    ProductResponse restock(Long id, RestockRequest request);

    ProductResponse findById(Long id);

    List<ProductResponse> listAll();

    List<ProductResponse> listLowStock();
}
