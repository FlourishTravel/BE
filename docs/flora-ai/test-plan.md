# Flora AI — Test Plan

## Automated (JUnit)

| Test class | Cases |
|------------|-------|
| `FloraPrivacyServiceTest` | Wrong user booking denied; consent required for location |
| `FloraLocationServiceTest` | Reject coords out of range; reject when consent off |
| `FloraReminderServiceTest` | Idempotency key prevents duplicate; threshold minutes |
| `FloraContextBuilderTest` | No other-user data in context; legacy chatbot request works |
| `ChatbotRequestCompatTest` | `content`-only payload still processes |

## Manual

1. Login → PATCH travel preferences → chat “Gợi ý tour cho tôi”.
2. Active booking → GET journey → verify countdown + meeting point.
3. POST location with consent → nearby suggestion in chat.
4. Disable location consent → POST location returns 403.
5. Reminder job → single notification per idempotency key.

## Regression

- Existing `POST /chatbot/message` without new fields unchanged.
- `/users/me` profile fields unchanged.
