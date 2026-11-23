package com.supportdesk.ticket.controller;

import com.supportdesk.ticket.dto.CategoryAdminDtos.CategoryDto;
import com.supportdesk.ticket.dto.CategoryAdminDtos.CreateCategoryRequest;
import com.supportdesk.ticket.dto.CategoryAdminDtos.CreateSubCategoryRequest;
import com.supportdesk.ticket.dto.CategoryAdminDtos.SubCategoryDto;
import com.supportdesk.ticket.service.CategoryAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@PreAuthorize("hasRole('GUIDE_ADMIN')")
@RequiredArgsConstructor
public class CategoryAdminController implements CategoryAdminApi
{
    private final CategoryAdminService categoryAdminService;

    @Override
    public List<CategoryDto> all()
    {
        return categoryAdminService.all();
    }

    @Override
    public CategoryDto createCategory(CreateCategoryRequest request)
    {
        return categoryAdminService.createCategory(request);
    }

    @Override
    public List<SubCategoryDto> subCategories(String code)
    {
        return categoryAdminService.subCategories(code);
    }

    @Override
    public SubCategoryDto createSubCategory(String code, CreateSubCategoryRequest request)
    {
        return categoryAdminService.createSubCategory(code, request);
    }
}
