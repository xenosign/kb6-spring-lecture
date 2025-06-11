<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ include file="/WEB-INF/header.jsp" %>
<html>
<head>
  <title>Member Add</title>
</head>
<body>
<h1>회원 추가 페이지</h1>
<form action="/member/add" method="post">
  이름 : <input type="text" id="name" name="name" required /><br />
  이메일 : <input type="text" id="email" name="email" required /><br />
  <br />
  <button type="submit">회원 추가</button>
</form>
</body>
</html>
