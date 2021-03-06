/*
 * Copyright 2015-2016 Jeeva Kandasamy (jkandasa@gmail.com)
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.mycontroller.standalone.gateway.mqtt;

import org.apache.commons.lang.StringUtils;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.mycontroller.standalone.AppProperties.STATE;
import org.mycontroller.standalone.gateway.model.GatewayMQTT;
import org.mycontroller.standalone.message.RawMessage;
import org.mycontroller.standalone.message.RawMessageQueue;

import lombok.extern.slf4j.Slf4j;

/**
 * @author Jeeva Kandasamy (jkandasa)
 * @since 0.0.2
 */
@Slf4j
public class MqttCallbackListener implements MqttCallback {
    private IMqttClient mqttClient;
    private GatewayMQTT gateway;
    private boolean reconnect = true;
    public static final long RECONNECT_WAIT_TIME = 1000 * 5;

    public MqttCallbackListener(IMqttClient mqttClient, GatewayMQTT gateway) {
        this.mqttClient = mqttClient;
        this.gateway = gateway;
    }

    @Override
    public void connectionLost(Throwable throwable) {
        _logger.error("MQTT GatewayTable[id:{}, Name:{}, serverURI:{}] connection lost! Error:{}",
                gateway.getId(), gateway.getName(), mqttClient.getServerURI(), throwable.getMessage());
        gateway.setStatus(STATE.DOWN, "ERROR: Connection lost! [" + throwable.getMessage() + "]");
        while (isReconnect()) {
            if (mqttClient.isConnected()) {
                break;
            } else {
                try {
                    mqttClient.connect();
                    _logger.info("MQTT GatewayTable[{}] Reconnected successfully...", mqttClient.getServerURI());
                    gateway.setStatus(STATE.UP, "Reconnected successfully...");
                } catch (MqttException ex) {
                    _logger.error("Exception, Reason Code:{}", ex.getReasonCode(), ex);
                }
                long waitTime = RECONNECT_WAIT_TIME;
                while (waitTime > 0 && isReconnect()) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ex) {
                        _logger.error("Exception, ", ex);
                    }
                }
            }
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken deliveryToken) {
        try {
            _logger.debug("Message Delivery Complete, [Message Id:{}, Topic:{}, PayLoad:{}]",
                    deliveryToken.getMessageId(),
                    StringUtils.join(deliveryToken.getTopics(), ","),
                    deliveryToken.getMessage());
        } catch (MqttException ex) {
            _logger.error("Exception, ", ex);
        }
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        try {
            _logger.debug("Message Received, Topic:[{}], PayLoad:[{}]", topic, message);
            RawMessageQueue.getInstance().putMessage(RawMessage.builder()
                    .gatewayId(gateway.getId())
                    .data(message.toString())
                    .subData(topic)
                    .networkType(gateway.getNetworkType())
                    .build());
        } catch (Exception ex) {
            _logger.error("Exception, ", ex);
        }
    }

    public boolean isReconnect() {
        return reconnect;
    }

    public synchronized void setReconnect(boolean reconnect) {
        this.reconnect = reconnect;
    }
}
