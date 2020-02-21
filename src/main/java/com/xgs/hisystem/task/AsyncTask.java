package com.xgs.hisystem.task;

import com.alibaba.fastjson.JSON;
import com.xgs.hisystem.config.Contants;
import com.xgs.hisystem.pojo.entity.LoginInforEntity;
import com.xgs.hisystem.pojo.entity.UserEntity;
import com.xgs.hisystem.pojo.vo.getAddress.GetAddressRspVO;
import com.xgs.hisystem.repository.ILoginInforRepository;
import com.xgs.hisystem.repository.IUserRepository;
import com.xgs.hisystem.service.impl.AdminServiceImpl;
import com.xgs.hisystem.util.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * @author xgs
 * @Description:
 * @date 2019/3/25
 */
@Component
public class AsyncTask {

    @Autowired
    private IUserRepository iUserRepository;

    @Autowired
    private ILoginInforRepository iLoginInforRepository;

    private static final Logger logger = LoggerFactory.getLogger(AsyncTask.class);

    /**
     * 保存登录信息 异步线程
     *
     * @param ip
     * @param broswer
     * @return
     */
    @Async("myTaskAsyncPool")
    public void saveLoginInfor(String ip, String broswer, String email) {

        UserEntity user = iUserRepository.findByEmail(email);
        String userId = user.getId();
        LoginInforEntity checkUserInformation = iLoginInforRepository.findByLoginIpAndLoginBroswerAndUserId(ip, broswer, userId);


        try {
            if (checkUserInformation == null) {

                LoginInforEntity userInformation = new LoginInforEntity();
                userInformation.setLoginIp(ip);
                userInformation.setLoginBroswer(broswer);
                userInformation.setUser(user);
                userInformation.setDescription(email);

                RestTemplate restTemplate = new RestTemplate();

                //调百度地图api，通过ip获取地理位置
                String url = Contants.url.BAIDU_URL + ip;

                String result = null;
                try {
                    result = restTemplate.getForObject(url, String.class);
                } catch (RestClientException e) {
                    e.printStackTrace();
                }

                if (!StringUtils.isEmpty(result)) {

                    GetAddressRspVO addressRspVO = JSON.parseObject(result, GetAddressRspVO.class);
                    String loginAddress = addressRspVO.getContent().getAddress();

                    userInformation.setLoginAddress(loginAddress);
                }
                iLoginInforRepository.saveAndFlush(userInformation);
            } else {
                checkUserInformation.setCreateDatetime(DateUtil.getCurrentDateToString());

                iLoginInforRepository.saveAndFlush(checkUserInformation);
            }
        } catch (Exception e) {
            logger.error("userId={},保存登录记录失败！msg={}", userId, e.getStackTrace());
        }

    }

}
