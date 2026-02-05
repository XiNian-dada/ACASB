from fastapi import FastAPI
import uvicorn

app = FastAPI()

@app.get("/ping")
async def ping():
    return {"message": "Python 引擎已就绪！可以开始计算色彩熵。"}

if __name__ == "__main__":
    uvicorn.run(app, host="127.0.0.1", port=5000)