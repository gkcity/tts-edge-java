module tts.edge.java {
    requires org.apache.commons.lang3;
    requires org.apache.commons.text;
    requires org.java_websocket;
    requires fastjson;

    exports com.github.gkcity.tts;
    exports com.github.gkcity.tts.bean;

    // 开放实体包给所有模块反射（便捷，无需指定fastjson）
    opens com.github.gkcity.tts;
    opens com.github.gkcity.tts.bean;
}