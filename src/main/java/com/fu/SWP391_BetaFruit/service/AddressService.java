package com.fu.SWP391_BetaFruit.service;

import com.fu.SWP391_BetaFruit.dto.request.AddressRequest;
import com.fu.SWP391_BetaFruit.dto.response.AddressResponse;
import com.fu.SWP391_BetaFruit.enums.AddressType;

import java.util.List;

public interface AddressService {

    void createAddress(Integer userId, AddressRequest addressRequest, AddressType forceType) throws Exception;

    List<AddressResponse> getAllAddressesByRole(Integer userId, String role) throws Exception;

    AddressResponse getAddressById(Integer userId, Integer addressId) throws Exception;

    void updateAddress(Integer userId, Integer addressId, AddressRequest request, AddressType forceType) throws Exception;

    void deleteAddress(Integer userId, Integer addressId) throws Exception;
}
