import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],

  server: {
    port: 5173,

    // ---------------------------------------------------------------------
    // 개발용 프록시 — CORS 를 발생시키지 않기 위한 장치다.
    //
    // 브라우저는 localhost:5173 으로만 요청하고, Vite 가 그 요청을
    // 백엔드(8080)로 대신 전달한다. 브라우저 입장에서는 출처가 하나이므로
    // 애초에 교차 출처(cross-origin) 요청이 아니고 CORS 설정이 불필요하다.
    //
    // 배포 시에는 이 빌드 결과물을 스프링 jar 안에 넣어 같은 도메인에서
    // 서비스하므로 역시 CORS 가 없다(D-001).
    // ---------------------------------------------------------------------
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      // 결제 성공/실패 리다이렉트는 백엔드가 받는다
      '/payments': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },

  build: {
    // 산출물은 frontend/dist 에 둔다. Gradle 이 이것을 jar 의 static 으로
    // 복사한다. 소스 트리(src/main/resources/static)에 직접 쓰지 않는 이유는
    // 빌드 산출물이 형상관리 대상에 섞이지 않게 하기 위해서다.
    outDir: 'dist',
    // 배포 후 문제 추적을 위해 소스맵을 남긴다.
    sourcemap: true,
  },
})
