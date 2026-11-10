package com.aristino.backend.service;

import com.aristino.backend.common.CurrentUserProvider;
import com.aristino.backend.dto.address.AddressRequest;
import com.aristino.backend.dto.address.AddressResponse;
import com.aristino.backend.entity.Address;
import com.aristino.backend.entity.User;
import com.aristino.backend.repository.AddressRepository;
import com.aristino.backend.repository.OrderRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AddressService {

    private final AddressRepository addressRepository;
    private final OrderRepository orderRepository;
    private final CurrentUserProvider currentUserProvider;

    // Dia chi mac dinh xep len dau, con lai theo thu tu tao
    public List<AddressResponse> getMyAddresses() {
        User user = currentUserProvider.getCurrentUser();
        return addressRepository.findByUserId(user.getId()).stream()
                .sorted(Comparator
                        .comparing((Address a) -> !Boolean.TRUE.equals(a.getIsDefault()))
                        .thenComparing(Address::getId))
                .map(AddressResponse::from)
                .toList();
    }

    public AddressResponse create(AddressRequest request) {
        User user = currentUserProvider.getCurrentUser();
        List<Address> existing = addressRepository.findByUserId(user.getId());

        // Dia chi dau tien luon la mac dinh; chon mac dinh thi bo mac dinh cu
        boolean makeDefault = Boolean.TRUE.equals(request.getIsDefault()) || existing.isEmpty();
        if (makeDefault) {
            clearDefault(existing);
        }

        Address address = Address.builder()
                .user(user)
                .recipientName(request.getRecipientName().trim())
                .phone(request.getPhone().trim())
                .province(request.getProvince().trim())
                .district(request.getDistrict().trim())
                .ward(request.getWard().trim())
                .detailAddress(request.getDetailAddress().trim())
                .isDefault(makeDefault)
                .build();

        return AddressResponse.from(addressRepository.save(address));
    }

    public AddressResponse update(Long id, AddressRequest request) {
        User user = currentUserProvider.getCurrentUser();
        Address address = findOwned(id, user);

        address.setRecipientName(request.getRecipientName().trim());
        address.setPhone(request.getPhone().trim());
        address.setProvince(request.getProvince().trim());
        address.setDistrict(request.getDistrict().trim());
        address.setWard(request.getWard().trim());
        address.setDetailAddress(request.getDetailAddress().trim());

        // Chi xu ly truong hop tick "mac dinh". Bo tick o dia chi dang la mac dinh thi giu nguyen
        // (de luon co 1 dia chi mac dinh); muon doi thi dat dia chi khac lam mac dinh
        if (Boolean.TRUE.equals(request.getIsDefault()) && !Boolean.TRUE.equals(address.getIsDefault())) {
            clearDefault(addressRepository.findByUserId(user.getId()));
            address.setIsDefault(true);
        }

        return AddressResponse.from(addressRepository.save(address));
    }

    public AddressResponse setDefault(Long id) {
        User user = currentUserProvider.getCurrentUser();
        Address address = findOwned(id, user);

        if (!Boolean.TRUE.equals(address.getIsDefault())) {
            clearDefault(addressRepository.findByUserId(user.getId()));
            address.setIsDefault(true);
        }
        return AddressResponse.from(addressRepository.save(address));
    }

    public void delete(Long id) {
        User user = currentUserProvider.getCurrentUser();
        Address address = findOwned(id, user);
        boolean wasDefault = Boolean.TRUE.equals(address.getIsDefault());

        // Don hang cu da co ban chup dia chi nen chi can bo tham chieu address_id
        orderRepository.detachAddress(id);
        addressRepository.delete(address);
        addressRepository.flush();

        // Xoa dung dia chi mac dinh -> chuyen mac dinh sang dia chi con lai (neu co)
        if (wasDefault) {
            addressRepository.findByUserId(user.getId()).stream()
                    .min(Comparator.comparing(Address::getId))
                    .ifPresent(next -> next.setIsDefault(true));
        }
    }

    // Dung noi bo trong OrderService de kiem tra dia chi co thuoc ve user dang dat hang khong
    public Address getOwnedAddressOrThrow(Long addressId) {
        return findOwned(addressId, currentUserProvider.getCurrentUser());
    }

    private Address findOwned(Long addressId, User user) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new EntityNotFoundException("Khong tim thay dia chi"));
        if (!address.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Dia chi nay khong thuoc ve ban");
        }
        return address;
    }

    private void clearDefault(List<Address> addresses) {
        addresses.forEach(a -> a.setIsDefault(false));
    }
}