<%@ page language="java" contentType="text/html;charset=UTF-8" pageEncoding="UTF-8"%>
<%@include file="../header.jsp"%>
<!DOCTYPE html>
<html>
<head>
    <title>Login</title>
</head>
<body>
<h1>SECURITY 로그인</h1>
<form action="/user/login" method="post">
    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
    아이디: <input type="text" name="username"><br>
    비밀번호: <input type="password" name="password"><br>
    <input type="submit" value="로그인"/>
</form>
</body>
</html>