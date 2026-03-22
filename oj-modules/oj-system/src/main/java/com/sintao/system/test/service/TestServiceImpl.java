package com.sintao.system.test.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sintao.system.test.domain.TestDomain;
import com.sintao.system.test.mapper.TestMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class TestServiceImpl implements ITestService{


    @Autowired
    private TestMapper testMapper;


    @Override
    public List<?> list() {
        return testMapper.selectList(new LambdaQueryWrapper<>());
    }

    @Override
    public String add() {
        log.info("娣诲姞娴嬭瘯");
        TestDomain testDomain = new TestDomain();
        testDomain.setTitle("娴嬭瘯");
        testDomain.setContent("娴嬭瘯uuid涓婚敭鐢熸垚");
        testMapper.insert(testDomain);
        return "娣诲姞鏁版嵁鎴愬姛";
    }
}

