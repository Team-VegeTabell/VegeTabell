package com.example.VegeTabell.app.service;

import com.example.VegeTabell.app.entity.Shop;
import com.example.VegeTabell.app.entity.User;
import com.example.VegeTabell.app.entity.type.UserRole;
import com.example.VegeTabell.app.form.SignupForm;
import com.example.VegeTabell.app.repository.ShopRepository;
import com.example.VegeTabell.app.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SignupService {

    private final UserRepository userRepository;
    private final ShopRepository shopRepository;
    private final PasswordEncoder passwordEncoder;

    public SignupService(UserRepository userRepository,
                          ShopRepository shopRepository,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.shopRepository = shopRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public boolean emailExists(String email) {
        return userRepository.findByEmail(email).isPresent();
    }

    @Transactional
    public User register(SignupForm form) {
        User user = new User();
        user.setEmail(form.getEmail());
        user.setPasswordHash(passwordEncoder.encode(form.getPassword()));
        user.setRole(form.getRole());
        user.setDisplayName(form.getDisplayName());
        userRepository.save(user);

        if (form.getRole() == UserRole.SELLER) {
            Shop shop = new Shop();
            shop.setUser(user);
            shop.setShopName(form.getShopName());
            shop.setAddress(form.getAddress());
            shop.setLatitude(form.getLatitude());
            shop.setLongitude(form.getLongitude());
            shop.setPickupNote(form.getPickupNote());
            shopRepository.save(shop);
        }

        return user;
    }
}
