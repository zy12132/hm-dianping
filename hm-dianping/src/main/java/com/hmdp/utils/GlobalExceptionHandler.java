package com.hmdp.utils;

import com.hmdp.dto.Result;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;


// 全局异常处理器扩展
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MyException.class)
    public Result handleCustomException(MyException ex) {
        //返回一个具体的HTTP状态码和响应体时
        return Result.fail(ex.getMessage());
    }

    //兜底策略，确保任何未预期或未被捕获的异常都能得到妥善处理。
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGeneralException(Exception ex) {
        return new ResponseEntity<>("Internal Server Error", HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
