package com.webecommerce.controller.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.webecommerce.constant.ModelConstant;
import com.webecommerce.dao.product.IProductDAO;
import com.webecommerce.dto.ProductDTO;
import com.webecommerce.filter.FilterProduct;
import com.webecommerce.filter.FilterProductVariant;
import com.webecommerce.paging.PageRequest;
import com.webecommerce.paging.Pageable;
import com.webecommerce.service.ICategoryService;
import com.webecommerce.service.IProductService;
import com.webecommerce.sort.Sorter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@WebServlet(urlPatterns = {"/danh-sach-san-pham"})
public class ProductController extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(ProductController.class);
    @Inject
    private IProductService productService;
    @Inject
    private ICategoryService categoryService;
    public static String sanitizeDoubleInput(String input) {
        if (input == null) return null;

        String decoded = input.trim();

        // Cố gắng decode nếu có chứa ký tự % và xem như URL-encoded
        if (decoded.contains("%")) {
            try {
                decoded = URLDecoder.decode(decoded, StandardCharsets.UTF_8.name()).trim();
            } catch (IllegalArgumentException | UnsupportedEncodingException e) {
                logger.warn("Skipping URL decoding due to invalid format: {}", input);
                // Giữ nguyên giá trị gốc không decode
            }
        }

        // Loại bỏ ký tự không hợp lệ trong số thực
        decoded = decoded.replaceAll("[^0-9+\\-\\.]", "");

        // Kiểm tra định dạng số
        if (!decoded.matches("^[+-]?\\d*(\\.\\d+)?$")) {
            logger.warn("Invalid or potentially malicious double input: {}", decoded);
            return null;
        }

        return decoded;
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        List<String> listNames = productService.getAllProductName();
        ProductDTO product = new ProductDTO();

        String category = request.getParameter("category");
        String brand = request.getParameter("brand");
        int page = Integer.parseInt(request.getParameter("page"));
        int maxPageItem = Integer.parseInt(request.getParameter("maxPageItem"));
//        String minPriceStr = (request.getParameter("minPrice"));
//        String maxPriceStr = request.getParameter("maxPrice");
        String tag = request.getParameter("tag");
        String sort = request.getParameter("sort");

        String searchName = request.getParameter("ten");

        product.setPage(page);
        product.setMaxPageItem(maxPageItem);

        int categoryId = -1;

        try {
            categoryId = Integer.parseInt(category);
        }
        catch (NumberFormatException e) {
            System.out.println(e);
        }

        double minPrice = Integer.MIN_VALUE;
        double maxPrice = Integer.MAX_VALUE;
        String minPriceStr = sanitizeDoubleInput(request.getParameter("minPrice"));
        String maxPriceStr = sanitizeDoubleInput(request.getParameter("maxPrice"));

        if (minPriceStr != null && minPriceStr.matches("^\\d+(\\.\\d+)?$")) {
            try {
                minPrice = Double.parseDouble(minPriceStr);
                if (minPrice < 0) {
                    minPrice = 0;
                }
            } catch (NumberFormatException e) {
                logger.error("Invalid minPrice format: {}", minPriceStr);
            }
        } else if (minPriceStr != null) {
            logger.warn("Potential malicious minPrice input: {}", minPriceStr);
        }

        if (maxPriceStr != null && maxPriceStr.matches("^\\d+(\\.\\d+)?$")) {
            try {
                maxPrice = Double.parseDouble(maxPriceStr);
                if (maxPrice < minPrice) {
                    maxPrice = minPrice;
                }
            } catch (NumberFormatException e) {
                logger.error("Invalid maxPrice format: {}", maxPriceStr);
            }
        } else if (maxPriceStr != null) {
            logger.warn("Potential malicious maxPrice input: {}", maxPriceStr);
        }

        if(sort != null) {
            product.setSortBy(sort);
        }


        Pageable pageable =new PageRequest(product.getPage(), product.getMaxPageItem(), new FilterProduct(categoryId, brand, tag),
                new FilterProductVariant(minPrice, maxPrice), new Sorter(product.getSortBy()));


        List<ProductDTO> productDTOList = productService.findAll(pageable);
        if (searchName != null && !searchName.isEmpty()) {
            productDTOList = productService.findAllByName(pageable, searchName);
        }
        product.setResultList(productDTOList);

        //nháp

        product.setTotalItem(productService.getTotalItems());

        //hết nháp
        product.setTotalPage(productService.setTotalPage(product.getTotalItem(),
                product.getMaxPageItem()));

        request.setAttribute(ModelConstant.MODEL2, productService.getBrands());
        request.setAttribute(ModelConstant.MODEL1, categoryService.findAll());

        request.setAttribute(ModelConstant.MODEL,product);
        request.setAttribute("listNames", listNames);
        request.getRequestDispatcher("/views/web/product-list.jsp").forward(request, response);
    }
}
