

replace the API key in the .env file with a valid one before running

```bash
DD_API_KEY=your_api_key
```

run it with 

```bash
docker compose up --build
```

start again the client in another terminal at your convenience to send more messages from client to server 

```bash
docker compose run client
```