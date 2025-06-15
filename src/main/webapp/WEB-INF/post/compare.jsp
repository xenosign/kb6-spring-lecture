<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/WEB-INF/header.jsp" %>
<html>
<head>
    <title>DB Compare</title>
</head>
<body>
<h1>Member List</h1>
<h2>데이터 ${count}건 조회</h2>
<h3>MySQL 속도 : ${mysqlTime} ms</h3>
<h3>Redis 속도 : ${redisTime} ms</h3>
</body>
</html>