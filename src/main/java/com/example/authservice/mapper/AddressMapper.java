package com.example.authservice.mapper;

import com.example.authservice.model.Address;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AddressMapper {

    // 실제 데이터 삽입을 수행하는 메서드 (반환타입은 void나 int)
    void insertAddress(Address address);

    // 기존의 save 메서드를 default 메서드로 구현하여 파라미터 객체를 반환
    default Address save(Address address) {
        insertAddress(address);
        return address;
    }

}
