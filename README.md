sudo apt install default-jdk -y

sudo nano /etc/systemd/system/localsecurecam.service

cd LocalSecureCam/backend

chmod +x mvnw

./mvnw clean package -DskipTests

sudo systemctl daemon-reexec

sudo systemctl daemon-reload

sudo systemctl enable localsecurecam

sudo systemctl status localsecurecam

sudo systemctl start localsecurecam

journalctl -u localsecurecam -f