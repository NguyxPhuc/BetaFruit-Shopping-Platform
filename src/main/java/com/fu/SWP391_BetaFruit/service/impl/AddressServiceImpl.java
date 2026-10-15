package com.fu.SWP391_BetaFruit.service.impl;

import com.fu.SWP391_BetaFruit.dto.request.AddressRequest;
import com.fu.SWP391_BetaFruit.dto.response.AddressResponse;
import com.fu.SWP391_BetaFruit.entity.Address;
import com.fu.SWP391_BetaFruit.entity.User;
import com.fu.SWP391_BetaFruit.enums.AddressType;
import com.fu.SWP391_BetaFruit.repository.AddressRepository;
import com.fu.SWP391_BetaFruit.service.AddressService;
import com.fu.SWP391_BetaFruit.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class AddressServiceImpl implements AddressService {
    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private UserService userService;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void createAddress(Integer userId, AddressRequest addressRequest, AddressType forceType) throws Exception {
        User user = userService.getUserById(userId);
        if (user == null) {
            throw new Exception("Không tìm thấy người dùng trong hệ thống.");
        }
        Boolean isDefault = addressRequest.getIsDefault();
        AddressType addressType = forceType != null ? forceType : AddressType.valueOf(addressRequest.getAddressType().toUpperCase());
        int countByUserIdAndAddressType = addressRepository.countByUser_UserIdAndAddressType(userId, addressType);
        if(countByUserIdAndAddressType == 0){
            isDefault = true;
        }
        if(countByUserIdAndAddressType > 0 && isDefault){
            addressRepository.clearDefaultAddress(userId, addressType);
        }
        Address address = new Address();
        address.setUser(user);
        address.setReceiverName(addressRequest.getReceiverName());
        address.setReceiverPhone(addressRequest.getReceiverPhone());
        address.setAddressLine(addressRequest.getAddressLine());
        address.setCity(addressRequest.getCity());
        address.setDistrict(addressRequest.getDistrict());
        address.setWard(addressRequest.getWard());
        address.setAddressType(addressType);
        address.setIsDefault(isDefault);
        address.setIsActive(true);
        addressRepository.save(address);
    }

    private AddressResponse toAddressResponse(Address address){
        AddressResponse addressResponse = new AddressResponse();
        addressResponse.setAddressId(address.getAddressId());
        addressResponse.setReceiverName(address.getReceiverName());
        addressResponse.setReceiverPhone(address.getReceiverPhone());
        addressResponse.setAddressLine(address.getAddressLine());
        addressResponse.setCity(address.getCity());
        addressResponse.setDistrict(address.getDistrict());
        addressResponse.setWard(address.getWard());
        addressResponse.setIsDefault(address.getIsDefault());
        addressResponse.setAddressType(address.getAddressType().toString());
        return addressResponse;
    }

    @Override
    public List<AddressResponse> getAllAddressesByRole(Integer userId, String role) throws Exception {
        if(role == null || role.isEmpty()){
            throw new Exception("Lỗi hệ thống không thể tìm thấy danh sách địa chỉ.");
        }
        List<Address> addresses = new ArrayList<>();
        if("customer".equals(role)){
            addresses = addressRepository.getAddressesByUser_UserIdAndIsActiveAndAddressTypeIn(userId, true, List.of(AddressType.SHIPPING));
        }
        if("shop".equals(role)){
            addresses = addressRepository.getAddressesByUser_UserIdAndIsActiveAndAddressTypeIn(userId, true, List.of(AddressType.PICKUP, AddressType.RETURN));
        }
        List<AddressResponse> addressResponseList = new ArrayList<>();
        for (Address address : addresses) {
            addressResponseList.add(toAddressResponse(address));
        }

        return addressResponseList;
    }

    @Override
    public AddressResponse getAddressById(Integer userId, Integer addressId) throws Exception {
        Address address = addressRepository.findByUser_UserIdAndAddressIdAndIsActive(userId, addressId, true)
                .orElseThrow(() -> new Exception("Không tìm thấy địa chỉ hoặc bạn không có quyền truy cập."));
        return toAddressResponse(address);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAddress(Integer userId, Integer addressId, AddressRequest request, AddressType forceType) throws Exception {
        Address address = addressRepository.findByUser_UserIdAndAddressIdAndIsActive(userId, addressId, true)
                .orElseThrow(() -> new Exception("Không tìm thấy địa chỉ."));
        AddressType type = (forceType != null) ? forceType : AddressType.valueOf(request.getAddressType().toUpperCase());
        boolean willBeDefault = request.getIsDefault() != null ? request.getIsDefault() : false;
        if (willBeDefault && !address.getIsDefault()) {
            addressRepository.clearDefaultAddress(userId, type);
        }
        address.setReceiverName(request.getReceiverName());
        address.setReceiverPhone(request.getReceiverPhone());
        address.setAddressLine(request.getAddressLine());
        address.setCity(request.getCity());
        address.setDistrict(request.getDistrict());
        address.setWard(request.getWard());
        address.setAddressType(type);
        address.setIsDefault(willBeDefault);
        addressRepository.save(address);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAddress(Integer userId, Integer addressId) throws Exception {
        Address address = addressRepository.findByUser_UserIdAndAddressIdAndIsActive(userId, addressId, true)
                .orElseThrow(() -> new Exception("Không tìm thấy địa chỉ."));
        address.setIsActive(false);
        if (address.getIsDefault()) {
            address.setIsDefault(false);
        }
        addressRepository.save(address);
    }
}
