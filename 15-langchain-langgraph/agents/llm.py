import os

from langchain_google_genai import ChatGoogleGenerativeAI


def get_model() -> ChatGoogleGenerativeAI:
    """Gemini 모델. 다른 모델로 바꾸려면 이 함수만 수정하면 된다.
    API 키는 환경변수 GOOGLE_API_KEY 로 읽는다 (.env 에 두고 커밋하지 않는다)."""
    return ChatGoogleGenerativeAI(
        model=os.getenv("MODEL", "gemini-3.5-flash"),
        temperature=0,
        timeout=60,       # 한 번의 호출이 60초를 넘기면 포기한다
        max_retries=2,    # 과부하(503) 때 재시도는 2번까지만. 기본값에 맡기면 오래 매달려 있을 수 있다
    )
