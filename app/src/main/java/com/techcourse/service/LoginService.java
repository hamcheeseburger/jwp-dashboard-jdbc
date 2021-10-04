package com.techcourse.service;

import com.techcourse.controller.UserSession;
import com.techcourse.controller.request.LoginRequest;
import com.techcourse.dao.UserDao;
import com.techcourse.domain.User;
import com.techcourse.exception.UnauthorizedException;
import jakarta.servlet.http.HttpSession;
import nextstep.web.annotation.Autowired;
import nextstep.web.annotation.Service;

@Service
public class LoginService {

    private final UserDao userDao;

    @Autowired
    public LoginService(UserDao userDao) {
        this.userDao = userDao;
    }

    public void login(LoginRequest request) {
        User user = findUserByAccount(request.getAccount());
        user.checkPassword(request.getPassword());

        HttpSession session = request.getHttpSession();
        session.setAttribute(UserSession.SESSION_KEY, user);
    }

    private User findUserByAccount(String account) {
        return userDao.findByAccount(account)
            .orElseThrow(() -> new UnauthorizedException(String.format("%s 계정의 유저가 존재하지 않습니다.", account)));
    }
}