package com.stockpilot.core.port.in;

import com.stockpilot.core.port.in.dto.ProductRequest;
import com.stockpilot.core.port.in.dto.ProductResponse;
import com.stockpilot.core.port.in.dto.ProductUpdateRequest;
import com.stockpilot.core.port.in.dto.RestockRequest;
import com.stockpilot.core.query.PageQuery;
import com.stockpilot.core.query.PageResult;
import com.stockpilot.core.query.ProductSearchCriteria;
import java.util.List;

public interface ManageProductsUseCase {

    ProductResponse register(ProductRequest request);

    ProductResponse update(Long id, ProductUpdateRequest request);

    ProductResponse deactivate(Long id);

    ProductResponse reactivate(Long id);

    ProductResponse restock(Long id, RestockRequest request);

    ProductResponse findById(Long id);

    PageResult<ProductResponse> search(ProductSearchCriteria criteria, PageQuery pageQuery);

    List<ProductResponse> listLowStock();
}
