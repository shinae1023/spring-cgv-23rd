package com.ceos.voteservice;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {
    @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
    @Operation(summary = "스웨거테스트")
    public String home() {
        return """
                <!DOCTYPE html>
                <html lang="ko">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Jobdri CEOS VOTE</title>
                    <style>
                        :root {
                            color-scheme: light;
                            --bg-start: #fff4db;
                            --bg-end: #ffd8c2;
                            --card: rgba(255, 252, 247, 0.82);
                            --text-main: #2d1f3d;
                            --text-sub: #6a5b74;
                            --accent: #ff7a59;
                            --accent-soft: #ffd166;
                            --line: rgba(255, 255, 255, 0.55);
                        }

                        * {
                            box-sizing: border-box;
                        }

                        body {
                            margin: 0;
                            min-height: 100vh;
                            display: flex;
                            align-items: center;
                            justify-content: center;
                            overflow: hidden;
                            font-family: "Trebuchet MS", "Apple SD Gothic Neo", "Malgun Gothic", sans-serif;
                            background:
                                radial-gradient(circle at top left, rgba(255, 255, 255, 0.95), transparent 28%),
                                radial-gradient(circle at bottom right, rgba(255, 255, 255, 0.7), transparent 30%),
                                linear-gradient(135deg, var(--bg-start), var(--bg-end));
                            color: var(--text-main);
                        }

                        .orb {
                            position: absolute;
                            border-radius: 999px;
                            filter: blur(6px);
                            opacity: 0.7;
                            animation: float 9s ease-in-out infinite;
                        }

                        .orb.one {
                            width: 220px;
                            height: 220px;
                            top: 8%;
                            left: 8%;
                            background: rgba(255, 209, 102, 0.55);
                        }

                        .orb.two {
                            width: 160px;
                            height: 160px;
                            right: 10%;
                            bottom: 12%;
                            background: rgba(255, 122, 89, 0.35);
                            animation-delay: -3s;
                        }

                        .shell {
                            position: relative;
                            width: min(92vw, 760px);
                            padding: 28px;
                        }

                        .card {
                            position: relative;
                            padding: 48px 34px;
                            border: 1px solid var(--line);
                            border-radius: 28px;
                            background: var(--card);
                            backdrop-filter: blur(14px);
                            box-shadow: 0 24px 80px rgba(95, 57, 46, 0.15);
                            text-align: center;
                            animation: rise 0.9s ease-out both;
                        }

                        .badge {
                            display: inline-flex;
                            align-items: center;
                            gap: 8px;
                            padding: 10px 16px;
                            border-radius: 999px;
                            background: rgba(255, 255, 255, 0.88);
                            color: var(--text-sub);
                            font-size: 0.92rem;
                            letter-spacing: 0.04em;
                            box-shadow: 0 10px 26px rgba(75, 47, 78, 0.08);
                            animation: pop 0.7s ease-out 0.2s both;
                        }

                        .badge::before {
                            content: "";
                            width: 10px;
                            height: 10px;
                            border-radius: 50%;
                            background: linear-gradient(135deg, var(--accent), var(--accent-soft));
                        }

                        h1 {
                            margin: 22px 0 16px;
                            font-size: clamp(2.1rem, 5vw, 4rem);
                            line-height: 1.08;
                            letter-spacing: -0.04em;
                        }

                        .highlight {
                            display: inline-block;
                            color: var(--accent);
                            animation: bob 2.6s ease-in-out infinite;
                        }

                        p {
                            max-width: 520px;
                            margin: 0 auto;
                            font-size: clamp(1rem, 2vw, 1.12rem);
                            line-height: 1.75;
                            color: var(--text-sub);
                            animation: fadeUp 0.8s ease-out 0.35s both;
                        }

                        .sparkles {
                            display: flex;
                            justify-content: center;
                            gap: 14px;
                            margin-top: 28px;
                        }

                        .sparkles span {
                            width: 12px;
                            height: 12px;
                            border-radius: 50%;
                            background: linear-gradient(135deg, var(--accent), var(--accent-soft));
                            animation: twinkle 1.8s ease-in-out infinite;
                        }

                        .sparkles span:nth-child(2) {
                            animation-delay: 0.25s;
                        }

                        .sparkles span:nth-child(3) {
                            animation-delay: 0.5s;
                        }

                        @keyframes rise {
                            from {
                                opacity: 0;
                                transform: translateY(26px) scale(0.97);
                            }
                            to {
                                opacity: 1;
                                transform: translateY(0) scale(1);
                            }
                        }

                        @keyframes fadeUp {
                            from {
                                opacity: 0;
                                transform: translateY(18px);
                            }
                            to {
                                opacity: 1;
                                transform: translateY(0);
                            }
                        }

                        @keyframes pop {
                            from {
                                opacity: 0;
                                transform: scale(0.9);
                            }
                            to {
                                opacity: 1;
                                transform: scale(1);
                            }
                        }

                        @keyframes float {
                            0%, 100% {
                                transform: translateY(0px) translateX(0px);
                            }
                            50% {
                                transform: translateY(-18px) translateX(10px);
                            }
                        }

                        @keyframes twinkle {
                            0%, 100% {
                                transform: translateY(0) scale(1);
                                opacity: 0.75;
                            }
                            50% {
                                transform: translateY(-6px) scale(1.15);
                                opacity: 1;
                            }
                        }

                        @keyframes bob {
                            0%, 100% {
                                transform: translateY(0);
                            }
                            50% {
                                transform: translateY(-5px);
                            }
                        }

                        @media (max-width: 640px) {
                            .shell {
                                padding: 18px;
                            }

                            .card {
                                padding: 40px 22px;
                                border-radius: 22px;
                            }

                            p {
                                line-height: 1.6;
                            }
                        }
                    </style>
                </head>
                <body>
                    <div class="orb one"></div>
                    <div class="orb two"></div>
                    <main class="shell">
                        <section class="card">
                            <div class="badge">CEOS VOTE SERVER</div>
                            <h1>
                                <span class="highlight">Jobdri</span> 팀의<br>
                                CEOS VOTE 서버입니다
                            </h1>
                            <p>
                                투표 API가 반갑게 대기 중입니다.
                                오늘의 선택도, 팀의 결정도 이 서버가 든든하게 받아둘게요.
                            </p>
                            <div class="sparkles" aria-hidden="true">
                                <span></span>
                                <span></span>
                                <span></span>
                            </div>
                        </section>
                    </main>
                </body>
                </html>
                """;
    }
}
