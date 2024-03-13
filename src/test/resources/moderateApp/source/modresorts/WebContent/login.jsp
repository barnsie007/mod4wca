<%--
  Created by IntelliJ IDEA.
  User: kieran
  Date: 13/04/2023
  Time: 16:42
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" session="false"%>
<html>
<head>
    <title>Login</title>
</head>
<body>

    <form action="j_security_check">
        <input type="text" name="j_username">
        <input type="password" name="j_password" autocomplete="off">
        <input type="submit" value="Log In"/>
    </form>

</body>
</html>
