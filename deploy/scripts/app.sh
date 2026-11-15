#!/bin/bash
APP_NAME=railway-ticket
APP_PORT=8080
JAR_FILE=demo2-0.0.1-SNAPSHOT.jar
PID_FILE=/var/run/${APP_NAME}.pid
LOG_DIR=/opt/${APP_NAME}/logs
JWT_SECRET=${JWT_SECRET:-railway_ticket_secret_key_2026_very_long_secret_for_jwt_signing}

mkdir -p ${LOG_DIR}

case "$1" in
    start)
        # prod profile 的数据源必须由环境变量提供，缺失时直接失败，避免"启动成功但连不上库"
        : "${SPRING_DATASOURCE_URL:?请先设置 SPRING_DATASOURCE_URL}"
        : "${SPRING_DATASOURCE_USERNAME:?请先设置 SPRING_DATASOURCE_USERNAME}"
        : "${SPRING_DATASOURCE_PASSWORD:?请先设置 SPRING_DATASOURCE_PASSWORD}"

        if [ -f "$PID_FILE" ] && kill -0 "$(cat $PID_FILE)" 2>/dev/null; then
            echo "⚠️  $APP_NAME 已在运行 (PID: $(cat $PID_FILE))"
            exit 0
        fi
        echo "🚀 启动 $APP_NAME..."
        nohup java -jar \
            -Xms256m -Xmx512m \
            -Dspring.profiles.active=prod \
            -Dserver.port=${APP_PORT} \
            -Djwt.secret=${JWT_SECRET} \
            ${JAR_FILE} \
            > ${LOG_DIR}/app.log 2>&1 &
        echo $! > $PID_FILE
        sleep 3
        if kill -0 "$(cat $PID_FILE)" 2>/dev/null; then
            echo "✅ 启动成功 (PID: $(cat $PID_FILE))"
            echo "   日志: tail -f ${LOG_DIR}/app.log"
        else
            echo "❌ 启动失败，查看日志:"
            tail -20 ${LOG_DIR}/app.log
            rm -f $PID_FILE
            exit 1
        fi
        ;;
    stop)
        if [ ! -f "$PID_FILE" ]; then
            echo "⚠️  $APP_NAME 未运行"
            exit 0
        fi
        PID=$(cat $PID_FILE)
        echo "🛑 停止 $APP_NAME (PID: $PID)..."
        kill $PID
        sleep 5
        if kill -0 $PID 2>/dev/null; then
            echo "强制终止..."
            kill -9 $PID
        fi
        rm -f $PID_FILE
        echo "✅ 已停止"
        ;;
    restart)
        $0 stop
        sleep 2
        $0 start
        ;;
    status)
        if [ -f "$PID_FILE" ] && kill -0 "$(cat $PID_FILE)" 2>/dev/null; then
            echo "✅ $APP_NAME 运行中 (PID: $(cat $PID_FILE), 端口: $APP_PORT)"
        else
            echo "❌ $APP_NAME 未运行"
        fi
        ;;
    *)
        echo "用法: $0 {start|stop|restart|status}"
        exit 1
        ;;
esac