package com.example.template.service;

import java.util.Map;

/**
 * 示例业务接口（业务层入口）。
 *
 * <p>分层约定：Controller 只做参数接收与校验，业务逻辑放在 Service。</p>
 */
public interface HelloService {

    /**
     * 生成问候语。
     *
     * @param name 名称
     * @return 问候语
     */
    String greet(String name);

    /**
     * 返回服务运行信息。
     *
     * @return 应用名、环境、状态、时间
     */
    Map<String, Object> serverInfo();
}
