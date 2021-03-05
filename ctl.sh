#!/bin/bash
set -e
ENVIR_PATH=$(realpath "$(dirname "$0")")/envir
if [ -f $ENVIR_PATH ]; then
. $ENVIR_PATH
else
  echo "Не найден файл окружения $ENVIR_PATH.">&2
  echo "Демо-файл $ENVIR_PATH будет создан. Пожалуйста, поправьте в нем что нужно.">&2
  {
    echo "# Окружение сервера. Установите все прокомментированные параметры."
    echo
    echo "# Домен установки"
    echo "export PO_DOMAIN=$(basename $(realpath "$(dirname "$0")"))"
    echo "export PO_DIR=\$HOME/\$PO_DOMAIN"
    echo "export PO_URL=https://\$PO_DOMAIN/"
    echo
    echo "# Порт веб-сервера"
    echo "export PO_APP_PORT=10008"
    echo "# Порт базы данных главного сервера"
    echo "export PO_DB_MASTER_PORT=10001"
    echo "# Порт базы данных резервного сервера"
    echo "export PO_DB_SLAVE_PORT=10002"
    echo
    echo "# Адрес главного сервера относительно резервного"
    echo "### export PO_MASTER_IP=10.8.68.2"
    echo "# Адрес резервного сервера относительно главного"
    echo "### export PO_SLAVE_IP=10.35.68.10"
    echo
    echo "export PO_DB_NAME=dots"
    echo "export PO_DB_USER=$USER"
    echo "# Пароль базы данных"
    echo "export PO_DB_PASS=$(dd if=/dev/urandom bs=12 count=1 status=none | base64 | tr -dc a-zA-Z0-9)"
    echo "export PO_DB_RO_USER=r_$USER"
    echo "# Пароль базы данных пользователя только для чтения"
    echo "export PO_DB_RO_PASS=$(dd if=/dev/urandom bs=12 count=1 status=none | base64 | tr -dc a-zA-Z0-9)"
    echo
    echo "### export PO_SLAVE_SSH=$USER@\$PO_SLAVE_IP"
    echo
    echo "export PO_IS_MASTER=1"
    echo
    echo "if [ \$PO_IS_MASTER = 1 ] ; then"
    echo "        export PGPORT=\$PO_DB_MASTER_PORT"
    echo "else"
    echo "        export PGPORT=\$PO_DB_SLAVE_PORT"
    echo "fi"
    echo "export PGHOST=127.0.0.1"
    echo "export PGDATABASE=\$PO_DB_NAME"
    echo "export PGUSER=\$PO_DB_USER"
    echo "export PGPASSWORD=\$PO_DB_PASS"
    echo "export PGDATA=\$PO_DIR/var/data"
  } > $ENVIR_PATH
  exit 1
fi

case "$1" in
  reconfig)
    if [ $PO_IS_MASTER = 1 ] ; then
      {
        echo "log.path.template=$PO_DIR/var/log/%yyyy-MM-dd%.app.log"
        echo "log.path.currentSymlink=$PO_DIR/var/log/current.app.log"
        echo "dots.url=https://$PO_DOMAIN/"
        echo "dots.port=$PO_APP_PORT"
        echo "dots.uploadPath=$PO_DIR/var/up/"
        echo "dots.dbWrite=jdbc:postgresql://127.0.0.1:$PO_DB_MASTER_PORT/$PO_DB_NAME?user=$PO_DB_USER&password=$PO_DB_PASS"
        echo "#dots.sipmarket.apiUrl=https://sipmarket.net/API/"
        echo "#dots.sipmarket.login="
        echo "#dots.sipmarket.apicode="
        echo "#dots.dbReadOnly=same ^"
        echo "#zDb.debugSql=/home/dima/Work/Dots/db.sql"
        echo "mail.smtp.auth=true"
        echo "mail.smtp.host=smtp.yandex.ru"
        echo "mail.smtp.port=465"
        echo "mail.smtp.from=user@example.com"
        echo "mail.smtp.user=user@example.com"
        echo "mail.smtp.password="
        echo "#mail.smtp.starttls.enable=true"
        echo "mail.smtp.socketFactory.port=465"
        echo "mail.smtp.socketFactory.class=javax.net.ssl.SSLSocketFactory"
        echo "mail.app.mailFrom=user@example.com"
        echo "mail.app.mailTechAddresses="
        echo "mail.app.mailBccAddresses="
      } > $PO_DIR/conf/app.properties

      {
        echo "index index.html;"
        echo "charset utf-8;"
        echo "source_charset utf-8;"
        echo "location /a/s/ {"
        echo "    proxy_http_version 1.1;"
        echo "    proxy_pass http://127.0.0.1:$PO_APP_PORT;"
        echo "    proxy_read_timeout 600;"
        echo "    proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;"
        echo "    proxy_set_header Host \$host;"
        echo "    proxy_set_header Upgrade \$http_upgrade;"
        echo "    proxy_set_header Connection \$http_connection;"
        echo "}"
        echo "location /a/ {"
        echo "    proxy_pass http://127.0.0.1:$PO_APP_PORT;"
        echo "    proxy_read_timeout 600;"
        echo "    proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;"
        echo "    proxy_set_header Host \$host;"
        echo "}"
        echo "location /a2/ {"
        echo "    proxy_pass http://127.0.0.1:$PO_APP_PORT;"
        echo "    proxy_read_timeout 600;"
        echo "    proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;"
        echo "    proxy_set_header Host \$host;"
        echo "}"
        echo "location /up/ {"
        echo "    root $PO_DIR/var;"
        echo "    try_files \$uri =404;"
        echo "}"
        echo "location /WEB-INF/ { deny all; }"
        echo "location / {"
        echo "    root $PO_DIR/app/front;"
        echo "    try_files \$uri \$uri/index.html =404;"
        echo "}"
      } > $PO_DIR/conf/nginx

      {
        echo "server {"
        echo "    listen 80;"
        echo "    server_name $PO_DOMAIN;"
        echo "    include $PO_DIR/conf/nginx;"
        echo "}"
      } > $PO_DIR/conf/$PO_DOMAIN.nginx

      {
        echo "data_directory = '$PO_DIR/var/data'"
        echo "hba_file = '$PO_DIR/conf/pg_hba.conf'"
        echo "ident_file = '$PO_DIR/conf/pg_ident.conf'"
        echo "external_pid_file = '$PO_DIR/var/pid.db'"
        echo "listen_addresses = '0.0.0.0'"
        echo "port = $PGPORT"
        echo "max_connections = 255"
        echo "superuser_reserved_connections = 3"
        echo "shared_buffers = 32MB"
        echo "maintenance_work_mem = 2MB"
        echo "work_mem=2MB"
        echo "log_min_duration_statement=3000"
        echo "dynamic_shared_memory_type = posix"
        echo "max_wal_senders = 5"
        echo "wal_level = replica"
        if [ -z "$PO_SLAVE_SSH" ]; then
          echo "Не установлена переменная PO_SLAVE_SSH, архивирование базы (archive_mode, archive_command) не будет включено" >&2
        else
          echo "archive_mode = on"
          echo "archive_command = 'scp -q %p \"$PO_SLAVE_SSH:$PO_DOMAIN/var/backup/xlog/%f\"'"
        fi
        echo "log_destination = 'stderr'"
        echo "logging_collector = on"
        echo "log_directory = '$PO_DIR/var/log'"
        echo "log_filename = '%Y-%m-%d.db.log'"
        echo "log_line_prefix = '%t [%p-%l] %q%u@%d '"
        echo "lc_messages = 'en_US.UTF-8'"
        echo "lc_monetary = 'en_US.UTF-8'"
        echo "lc_numeric = 'en_US.UTF-8'"
        echo "lc_time = 'en_US.UTF-8'"
        echo "deadlock_timeout = 1s"
        echo "track_activity_query_size=102400"
      } > $PO_DIR/conf/postgresql.conf

      {
        echo "# nothing"
      } > $PO_DIR/conf/pg_ident.conf

      {
        echo "#host all all 127.0.0.1/32 trust"
        echo "local postgres $PGUSER md5"
        echo "host postgres $PGUSER 0.0.0.0/0 md5"
        echo "host $PGDATABASE $PGUSER 0.0.0.0/0 md5"
        echo "host replication $PGUSER 0.0.0.0/0 md5"
      } > $PO_DIR/conf/pg_hba.conf
    else
      {
        echo "data_directory = '$PO_DIR/var/data'"
        echo "hba_file = '$PO_DIR/conf/pg_hba.conf'"
        echo "ident_file = '$PO_DIR/conf/pg_ident.conf'"
        echo "external_pid_file = '$PO_DIR/var/pid.db'"
        echo "listen_addresses = '0.0.0.0'"
        echo "port = $PGPORT"
        echo "max_connections = 255"
        echo "superuser_reserved_connections = 3"
        echo "shared_buffers = 32MB"
        echo "maintenance_work_mem = 2MB"
        echo "work_mem=2MB"
        echo "log_min_duration_statement=3000"
        echo "dynamic_shared_memory_type = posix"
        echo "max_wal_senders = 5"
        echo "wal_level = replica"
        echo "hot_standby = on"
        echo "hot_standby_feedback = on"
        echo "recovery_target_timeline = 'latest'"
        echo "primary_conninfo = 'host=$PO_MASTER_IP port=$PO_DB_MASTER_PORT user=$PO_DB_USER password=$PO_DB_PASS'"
        echo "restore_command = 'cp $PO_DIR/var/backup/xlog/%f %p'"
        echo "log_destination = 'stderr'"
        echo "logging_collector = on"
        echo "log_directory = '$PO_DIR/var/log'"
        echo "log_filename = '%Y-%m-%d.db.log'"
        echo "log_line_prefix = '%t [%p-%l] %q%u@%d '"
        echo "lc_messages = 'en_US.UTF-8'"
        echo "lc_monetary = 'en_US.UTF-8'"
        echo "lc_numeric = 'en_US.UTF-8'"
        echo "lc_time = 'en_US.UTF-8'"
        echo "deadlock_timeout = 1s"
        echo "track_activity_query_size=102400"
      } > $PO_DIR/conf/postgresql.conf

      {
        echo "# nothing"
      } > $PO_DIR/conf/pg_ident.conf

      {
        echo "#host all all 127.0.0.1/32 trust"
        echo "local postgres $PGUSER md5"
        echo "host postgres $PGUSER 0.0.0.0/0 md5"
        echo "host $PGDATABASE $PGUSER 0.0.0.0/0 md5"
        echo "host $PGDATABASE $PO_DB_RO_USER 0.0.0.0/0 md5"
        echo "host replication $PGUSER 0.0.0.0/0 md5"
      } > $PO_DIR/conf/pg_hba.conf

      touch $PO_DIR/var/data/standby.signal
    fi
  ;;

  db-start)
    /opt/postgres/bin/pg_ctl -o "--config-file=$PO_DIR/conf/postgresql.conf" -w start
  ;;

  db-stop)
    /opt/postgres/bin/pg_ctl -o "--config-file=$PO_DIR/conf/postgresql.conf" -w stop
  ;;

  db-restart)
    $0 db-stop
    $0 db-start
  ;;

  db-sql)
    /opt/postgres/bin/psql
  ;;

  db-psql)
    /opt/postgres/bin/psql postgres
  ;;

  db-backup)
    cd $PO_DIR/var/backup/full
    FNO=`date +%u-%H`.txz
    FN=$FNO
    set -e
    rm -rf temp
    /opt/postgres/bin/pg_basebackup --host=$PGHOST --port=$PGPORT --pgdata temp
    XZ_OPT=-1 tar -cJf $FN.tmp -C temp .
    if [ -f $FN.ok ]; then rm -f $FN.ok; fi
    mv -f $FN.tmp $FN.ok
    if [ -f $FN ]; then rm -f $FN; fi
    mv -f $FN.ok $FN
    ln -f -T -r -s $FN 0-last.txz
    echo $FNO > 0-name.txt
  ;;

  db-init)
    if [ $PO_IS_MASTER = 1 ] ; then
      echo -n "$PGPASSWORD" > "$PO_DIR/pg.pwd"
      chmod 0600 "$PO_DIR/pg.pwd"
      /opt/postgres/bin/initdb \
      --auth-host=md5 \
      --auth-local=md5 \
      --auth=md5 \
      --username=$PGUSER \
      --encoding=UTF-8 \
      --local=ru_RU.UTF-8 \
      --lc-messages=en_US.UTF-8 \
      --lc-monetary=en_US.UTF-8 \
      --lc-numeric=en_US.UTF-8 \
      --lc-time=en_US.UTF-8 \
      --lc-collate=ru_RU.UTF-8 \
      --lc-ctype=ru_RU.UTF-8 \
      --pwfile="$PO_DIR/pg.pwd"
      echo XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX > "$PO_DIR/pg.pwd"
      rm -f "$PO_DIR/pg.pwd"
      rm -f $PO_DIR/var/data/postgresql.auto.conf
      rm -f $PO_DIR/var/data/postgresql.conf
      rm -f $PO_DIR/var/data/pg_hba.conf
      rm -f $PO_DIR/var/data/pg_ident.conf

      $0 db-start
    else
      echo "Нет смысла инициализировать slave-кластер" >&2
      exit 1;
    fi
  ;;

  db-ro-grant)
    /opt/postgres/bin/psql $PO_DB_NAME -c "DROP OWNED BY $PO_DB_RO_USER;GRANT SELECT ON ALL TABLES IN SCHEMA public TO $PO_DB_RO_USER;"
  ;;

  db-create)
    /opt/postgres/bin/psql postgres -c "CREATE DATABASE $PO_DB_NAME OWNER $PO_DB_USER ENCODING 'UTF-8' LC_COLLATE='ru_RU.UTF-8' LC_CTYPE='ru_RU.UTF-8'"
    /opt/postgres/bin/psql $PO_DB_NAME -c "CREATE ROLE $PO_DB_RO_USER WITH LOGIN PASSWORD '$PO_DB_RO_PASS'"
    $0 db-ro-grant
  ;;

  db-dump)
    /opt/postgres/bin/pg_dump --create --clean --no-owner --disable-triggers --quote-all-identifiers --if-exists --no-sync $PO_DB_NAME
  ;;

  db-to-test)
      /opt/postgres/bin/psql -c "UPDATE common SET icon = REGEXP_REPLACE(icon, '^(.*)\s([^\s]+)$', '\1 #000'), label = REGEXP_REPLACE(label, '^(?:(?:TEST|DEV)\s+)?(.*)$', 'TEST \1') RETURNING icon, label, code"
  ;;

  db-to-dev)
      /opt/postgres/bin/psql -c "UPDATE common SET icon = REGEXP_REPLACE(icon, '^(.*)\s([^\s]+)$', '\1 #666'), label = REGEXP_REPLACE(label, '^(?:(?:TEST|DEV)\s+)?(.*)$', 'DEV \1') RETURNING icon, label, code"
  ;;

  app-start|app-stop|app-status)
    PO_SIGN="Main $PO_DIR/conf/app.properties"
    PO_APP_COMMAND="/opt/java/bin/java $JAVA_OPTS -cp '$PO_DIR/app/*' Main $PO_DIR/conf/app.properties"
    PO_APP_COUNT=`pgrep -cf " $PO_SIGN" || true`
    case "$1" in
      app-status)
        if [ $PO_APP_COUNT -gt 0 ]; then
          echo "Сервер запущен:" >&2
          (pgrep -af " $PO_SIGN" || true) >&2
        else
          echo "Сервер не запущен." >&2
        fi
        exit 0
      ;;
      app-start)
        if [ $PO_APP_COUNT -gt 0 ]; then
          echo "Сервер уже запущен:" >&2
          (pgrep -af " $PO_SIGN" || true) >&2
          exit 1
        fi
        echo "Запуск сервера..."
        eval "nohup $PO_APP_COMMAND >>$PO_DIR/var/log/app.out 2>&1 </dev/null &"
        PO_PID=`pgrep -f " $PO_SIGN"`
        echo "PID: $PO_PID"
        pgrep -af " $PO_SIGN"
        echo "Ожидание на 127.0.0.1:$PO_APP_PORT..."
        tail --pid=$PO_PID -n 0 -f $PO_DIR/var/log/app.out $PO_DIR/var/log/current.app.log &
        TAIL_PID=$!
        for i in {1..60} ; do
          if [ `nc -z 127.0.0.1 $PO_APP_PORT && echo 1 || echo 0` -eq 1 ]; then
              { kill -9 $TAIL_PID && wait $TAIL_PID || true; } >/dev/null 2>/dev/null
              echo "Запущено"
              exit 0
          fi
          if [ `pgrep -cf " $PO_SIGN" || true` -eq 0 ]; then
              sleep 1
              { kill -9 $TAIL_PID && wait $TAIL_PID || true; } >/dev/null 2>/dev/null
              echo "Не удалось запустить - процесс сервера завершился"
              exit 1
          fi
          sleep .5
        done
        { kill -9 $TAIL_PID && wait $TAIL_PID || true; } >/dev/null 2>/dev/null
        echo "Не дождались запуска"
        exit 1
      ;;
      app-stop)
        if [ $PO_APP_COUNT -eq 0 ]; then
          echo "Сервер не запущен." >&2
          exit 1
        fi
        echo "Отправка TERM..."
        (pgrep -af " $PO_SIGN" || true) >&2
        pkill -ef " $PO_SIGN"
        echo -n "Ожидание..."
        for i in {1..30} ; do
          if [ `pgrep -cf " $PO_SIGN" || true` -eq 0 ]; then
              echo "Завершено"
              exit 0
          fi
          echo -n .
          sleep 1
        done
        echo
        echo "Отправка KILL..."
        pkill -9 -ef " $PO_SIGN"
        echo -n "Ожидание..."
        for i in {1..30} ; do
          if [ `pgrep -cf " $PO_SIGN" || true` -eq 0 ]; then
              echo "Завершено"
              exit 0
          fi
          echo -n .
          sleep 1
        done
        echo
        echo "Не получилось."
        exit 1
      ;;
    esac

  ;;

  app-restart)
    $0 app-stop
    $0 app-start
  ;;

#  app-download)
#    mkdir -p ~/.DotsApp
#    pushd ~/.DotsApp > /dev/null
#    rm -f DotsServer.txt
#    wget https://example.com/dist/DotsServer.txt -q
#    cat DotsServer.txt | while read p; do
#      if [[ $p =~ (([a-f0-9]+)[ ]+\*?([A-Za-z0-9.]+)) ]]; then
#        FN="${BASH_REMATCH[3]}"
#        HASH="${BASH_REMATCH[2]}"
#        if [ -f "$FN"  ] && [ "$HASH" = "$(sha256sum -b "$FN" | cut -d" " -f1)" ]; then
#          echo "File $FN is up to date"
#        else
#          echo "Download $FN..."
#          wget -nv "https://example.com/dist/$FN" -O "$FN"
#        fi
#      else
#        echo "Wrong line in DotsServer.txt: '$p'"
#        exit 1
#      fi
#    done
#    echo "Download done"
#    popd > /dev/null
#  ;;

  app-extract)
    echo "Очистка app_old, app_new..."
    rm -rf "$PO_DIR/app_new"
    rm -rf "$PO_DIR/app_old"
    echo "Распаковка в app_new..."
    mkdir -p "$PO_DIR/app_new"
    pushd "$PO_DIR/app_new" > /dev/null
    cat ~/.DotsApp/DotsServer.txt | while read p; do
      if [[ $p =~ (([a-f0-9]+)[ ]+\*?([A-Za-z0-9.]+)) ]]; then
        FNAME="${BASH_REMATCH[3]}"
        FN=~/.DotsApp/$FNAME
        HASH="${BASH_REMATCH[2]}"
        if [ -f "$FN"  ] && [ "$HASH" = "$(sha256sum -b "$FN" | cut -d" " -f1)" ]; then
          cp $FN ./$FNAME
        else
          echo "File is not found: $FN..."
        fi
      else
        echo "Wrong line '$p'"
        exit 1
      fi
    done
    echo "Copy done"
    if [ -f DotsServer.sh ]; then
      echo "Execute DotsServer.sh"
      . DotsServer.sh
    else
      echo "DotsServer.sh is not found"
    fi
    popd > /dev/null
    echo "Extract complete"
  ;;

  app-update|app-update-restart)
    $0 app-extract
    if [ "$1" = "app-update-restart" ] ; then
      echo "Остановка..."
      $0 app-stop
    fi
    echo "Обновление папки app..."
    mv "$PO_DIR/app" "$PO_DIR/app_old"
    mv "$PO_DIR/app_new" "$PO_DIR/app"
    if [ "$1" = "app-update-restart" ] ; then
      echo "Запуск..."
      $0 app-start
    fi
  ;;

  app-webupdate|app-webupdate-restart)
    $0 app-download
    if [ "$1" = "app-webupdate-restart" ] ; then
      $0 app-update-restart
    else
      $0 app-update
    fi
  ;;

  init)
    mkdir -p $PO_DIR
    mkdir -p $PO_DIR/conf
    mkdir -p $PO_DIR/var
    mkdir -p $PO_DIR/var/backup
    mkdir -p $PO_DIR/var/backup/full
    mkdir -p $PO_DIR/var/backup/xlog
    mkdir -p $PO_DIR/var/data
    chmod 0700 "$PO_DIR/var/data"
    mkdir -p $PO_DIR/var/log

    if [ $PO_IS_MASTER = 1 ] ; then
      if [ -n "$PO_SLAVE_SSH" ]; then
        if [ ! -f "$HOME/.ssh/id_rsa" ]; then
          ssh-keygen -b 4096 -t rsa -q -f "$HOME/.ssh/id_rsa" -N ""
        fi
        if [ ! -z "$PO_SLAVE_SSH" ]; then
          echo "Проверка соединения к архивному серверу: $PO_SLAVE_SSH"
          ssh-copy-id "$PO_SLAVE_SSH"
          echo "Создание пути к каталогу xlog: $PO_SLAVE_SSH:$PO_DOMAIN/var/backup/xlog"
          ssh "$PO_SLAVE_SSH" mkdir -p "$PO_DOMAIN/var/backup/xlog"
        fi
      fi
      mkdir -p $PO_DIR/app
      mkdir -p $PO_DIR/var/up

      $0 reconfig
      $0 app-update
    else
      $0 reconfig
    fi
  ;;

  init-slave)
    if [ -n "$PO_SLAVE_SSH" ]; then
      scp -q $0 $PO_SLAVE_SSH:ctl
      ssh "$PO_SLAVE_SSH" bash -e -c "'mkdir -p "$PO_DOMAIN"; ln -f -s ../ctl $PO_DOMAIN/ctl'"
      sed 's/PO_IS_MASTER=1/PO_IS_MASTER=0/' $PO_DIR/envir > $PO_DIR/envir.slave
      scp -q $PO_DIR/envir.slave $PO_SLAVE_SSH:$PO_DOMAIN/envir
      rm $PO_DIR/envir.slave
      ssh "$PO_SLAVE_SSH" $PO_DOMAIN/ctl init
    else
      echo "Нет параметра PO_SLAVE_SSH" >&2
      exit 1;
    fi
  ;;

  db-to-slave)
    if [ ! -L "$PO_DIR/var/backup/full/0-last.txz" ]; then
      echo Create backup...
      $0 db-backup
    fi
    BACKUP_PATH=`readlink -f "$PO_DIR/var/backup/full/0-last.txz"`
    echo Copy backup $BACKUP_PATH to slave...
    scp -q $BACKUP_PATH $PO_SLAVE_SSH:$PO_DOMAIN/var/data/master.txz
    echo Extract...
    ssh "$PO_SLAVE_SSH" bash -e -c "'cd $PO_DOMAIN/var/data ; tar xJf master.txz; rm master.txz'"
  ;;

  su-init)
    echo "Install packages..."
    apt install -y libreadline-dev zlib1g-dev wget
    cd /opt
    echo "Download opt.txz..."
    rm -f "opt.txz"
    wget -nv https://example.com/dist/opt.txz -O "opt.txz"
    echo "Extract opt.txz..."
    tar xJf opt.txz
    rm opt.txz
  ;;

  ctl-webupdate)
    rm -f "$PO_DIR/ctl.new"
    wget -nv https://example.com/dist/ctl.sh -O "$PO_DIR/ctl.new"
    cp "$PO_DIR/ctl" "$PO_DIR/ctl.old"
    cat "$PO_DIR/ctl.new" > "$PO_DIR/ctl"
    rm -f "$PO_DIR/ctl.new"
  ;;

  *)
  echo "ctl <command>" >&2
  echo "  где <command>:" >&2
  echo "    reconfig - перезапись всех конфигураций в положенные места" >&2
  echo "    db-start - запуск БД" >&2
  echo "    db-stop - остановка БД" >&2
  echo "    db-restart - перезапуск БД" >&2
  echo "    db-sql - запуск утилиты psql на базе $PGDATABASE" >&2
  echo "    db-psql - запуск утилиты psql на базе postgres" >&2
  echo "    db-backup - запуск резервного копирования БД" >&2
  echo "    db-init - инициализация кластера БД" >&2
  echo "    db-ro-grant - Дать права на чтение пользователю $PO_DB_RO_USER" >&2
  echo "    db-create - создание новой, пустой БД" >&2
  echo "    db-dump - SQL-дамп базы в стандартный вывод" >&2
  echo "    db-to-test - Обновление таблицы common icon -> #000, label -> TEST" >&2
  echo "    db-to-dev - Обновление таблицы common icon -> #666, label -> DEV" >&2
  echo "    app-start - запуск приложения" >&2
  echo "    app-stop - остановка приложения" >&2
  echo "    app-restart - перезапуск приложения" >&2
  echo "    app-download - загрузка приложения в папку ~/.DotsApp" >&2
  echo "    app-extract - извлечение приложения из папки ~/.DotsApp в папку app_new" >&2
  echo "    app-update - обновление приложения из папки ~/.DotsApp" >&2
  echo "    app-webupdate - обновление приложения из интернета" >&2
  echo "    app-update-restart - app-update с перезапуском" >&2
  echo "    app-webupdate-restart - app-webupdate с перезапуском" >&2
  echo "    ctl-webupdate - обновление скрипта ctl из интернета" >&2
  echo "    init - скачивание и настройка окружения данной установки" >&2
  echo "    init-slave - настройка ведомого" >&2
  echo "    su-init - скачивание и настройка общего окружения (postgres12, tomcat9, java8, ...) - единственная комманда, требующая root-прав" >&2
  exit 3
  ;;

esac
