package com.techcourse.service;

import com.techcourse.controller.LoginController;
import com.techcourse.controller.request.RegisterRequest;
import com.techcourse.dao.UserDao;
import com.techcourse.domain.User;
import com.techcourse.exception.DuplicateAccountException;
import nextstep.web.annotation.Autowired;
import nextstep.web.annotation.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class RegisterService {

    private static final Logger LOG = LoggerFactory.getLogger(LoginController.class);

    private final UserDao userDao;

    @Autowired
    public RegisterService(UserDao userDao) {
        this.userDao = userDao;
    }

    public void registerUser(RegisterRequest request) {
        User user = request.toEntity();
        validateDuplicate(user);

        userDao.insert(user);
    }

    private void validateDuplicate(User user) {
        userDao.findByAccount(user.getAccount())
            .ifPresent(foundUser -> {
                LOG.debug("Duplicate account already exist => {}", foundUser.getAccount());
                throw new DuplicateAccountException(String.format("%s 와 동일한 계정이 존재합니다.", foundUser.getAccount()));
            });
    }
}