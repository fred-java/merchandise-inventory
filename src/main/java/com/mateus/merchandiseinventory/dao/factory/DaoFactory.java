package com.mateus.merchandiseinventory.dao.factory;

import com.mateus.merchandiseinventory.dao.ProductDao;

public interface DaoFactory {

    ProductDao createProductDao();
}
