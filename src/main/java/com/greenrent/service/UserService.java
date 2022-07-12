package com.greenrent.service;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import com.greenrent.dto.request.UpdatePasswordRequest;
import com.greenrent.dto.request.UserUpdateRequest;
import com.greenrent.exception.BadRequestException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.greenrent.domain.Role;
import com.greenrent.domain.User;
import com.greenrent.domain.enums.RoleType;
import com.greenrent.dto.UserDTO;
import com.greenrent.dto.mapper.UserMapper;
import com.greenrent.dto.request.RegisterRequest;
import com.greenrent.exception.ConflictException;
import com.greenrent.exception.ResourceNotFoundException;
import com.greenrent.exception.message.ErrorMessage;
import com.greenrent.repository.RoleRepository;
import com.greenrent.repository.UserRepository;
import lombok.AllArgsConstructor;

import javax.transaction.Transactional;

@Service
@AllArgsConstructor
public class UserService {
    private UserRepository userRepository;

    private RoleRepository roleRepository;

    private PasswordEncoder passwordEncoder;

    private UserMapper userMapper;


    public void register(RegisterRequest registerRequest) {
        if(userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new ConflictException(String.format(ErrorMessage.EMAIL_ALREADY_EXIST,registerRequest.getEmail()));
        }

        String encodedPassword= passwordEncoder.encode(registerRequest.getPassword());


        Role role= roleRepository.findByName(RoleType.ROLE_CUSTOMER).
                orElseThrow(()->new ResourceNotFoundException
                        (String.format(ErrorMessage.ROLE_NOT_FOUND_MESSAGE,RoleType.ROLE_CUSTOMER.name())));

        Set<Role> roles=new HashSet<>();
        roles.add(role);

        User user=new User();
        user.setFirstName(registerRequest.getFirstName());
        user.setLastName(registerRequest.getLastName());
        user.setEmail(registerRequest.getEmail());
        user.setPassword(encodedPassword);
        user.setPhoneNumber(registerRequest.getPhoneNumber());
        user.setAddress(registerRequest.getAddress());
        user.setZipCode(registerRequest.getZipCode());
        user.setRoles(roles);

        userRepository.save(user);
    }

    public List<UserDTO> getAllUsers(){
        List<User> users= userRepository.findAll();
        return userMapper.map(users);
    }


    public Page<UserDTO> getUserPage(Pageable pageable){
        Page<User> users= userRepository.findAll(pageable);
        Page<UserDTO> dtoPage= users.map(new Function<User, UserDTO>() {

            @Override
            public UserDTO apply(User user) {
                return userMapper.userToUserDTO(user);
            }
        });

        return dtoPage;
    }

    public UserDTO findById(Long id) {
        User user=userRepository.findById(id).orElseThrow(()->
                new ResourceNotFoundException(String.format(ErrorMessage.RESOURCE_NOT_FOUND_MESSAGE, id)));

        return userMapper.userToUserDTO(user);
    }

    // Edit Possword
    public void updatePassword(Long id, UpdatePasswordRequest passwordRequest) {
        Optional<User> userOpt= userRepository.findById(id);
        User user= userOpt.get();


        if(user.getBuiltIn()) {
            throw new BadRequestException(ErrorMessage.NOT_PERMITTED_METHOD_MESSAGE);
        }


 //       if(!BCrypt.hashpw(passwordRequest.getOldPassword(), user.getPassword()).equals(user.getPassword())) {
 //           throw new BadRequestException(ErrorMessage.PASSWORD_NOT_MATCHED);
 //       }

        if(!passwordEncoder.matches(passwordRequest.getOldPassword(),user.getPassword())) {
            throw new BadRequestException(ErrorMessage.PASSWORD_NOT_MATCHED);
        }

        String hashedPassword=passwordEncoder.encode(passwordRequest.getNewPassword());
        user.setPassword(hashedPassword);

        userRepository.save(user);

    }


    // Transactional annotasyonu bu method icin aicilir, method bitince kapanir.
    // JPA ile calisirken bu annotasyon ile transaction.start, tx commit, tx.stop islemlerini yapiyor.
    @Transactional
    public void updateUser(Long id, UserUpdateRequest userUpdateRequest) {
        boolean emailExist = userRepository.existsByEmail(userUpdateRequest.getEmail());
        User user = userRepository.findById(id).get();

        if(user.getBuiltIn()) {
            throw new BadRequestException(ErrorMessage.NOT_PERMITTED_METHOD_MESSAGE);
        }

        if(emailExist && !userUpdateRequest.getEmail().equals(user.getEmail())) {
            throw new ConflictException(ErrorMessage.EMAIL_ALREADY_EXIST);
        }

        userRepository.update(id,userUpdateRequest.getFirstName(),userUpdateRequest.getLastName(),
                userUpdateRequest.getPhoneNumber(),userUpdateRequest.getEmail(),userUpdateRequest.getAddress(),userUpdateRequest.getZipCode());

    }

    // Delete user
    public void removeById(Long id) {
        User user=userRepository.findById(id).orElseThrow(()->new
                ResourceNotFoundException(String.format(ErrorMessage.RESOURCE_NOT_FOUND_MESSAGE,id)));

        if(user.getBuiltIn()) {
            throw new BadRequestException(ErrorMessage.NOT_PERMITTED_METHOD_MESSAGE);
        }

        userRepository.deleteById(id);
    }


}