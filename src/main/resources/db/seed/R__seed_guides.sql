-- Local guide library (local profile only). Real guides are written and published through E7 authoring.
-- "Login failed: server error" has a retired v1, the live v2 and a v3 draft, so only v2 is ever recommended.

INSERT INTO guides (title, category_id, created_at, updated_at)
SELECT g.title, c.id, now(), now()
FROM (VALUES
    ('Login failed: server error',   'LOGIN_FAILED'),
    ('Login failed: general checks', 'LOGIN_FAILED'),
    ('Payment failed',               'PAYMENT_FAILED')
) AS g (title, category_code)
JOIN ticket_categories c ON c.code = g.category_code
ON CONFLICT (title) DO NOTHING;

INSERT INTO guide_platforms (guide_id, platform)
SELECT g.id, p.platform
FROM guides g
CROSS JOIN (VALUES ('MOBILE'), ('WEB')) AS p (platform)
WHERE g.title IN ('Login failed: server error', 'Login failed: general checks', 'Payment failed')
ON CONFLICT DO NOTHING;

INSERT INTO guide_error_codes (guide_id, error_code)
SELECT g.id, e.error_code
FROM (VALUES
    ('Login failed: server error', 'ERR_AUTH_503'),
    ('Payment failed',             'PAY_TIMEOUT')
) AS e (title, error_code)
JOIN guides g ON g.title = e.title
ON CONFLICT DO NOTHING;

INSERT INTO guide_versions (guide_id, version_number, state, tree, published_at, created_at, updated_at)
SELECT g.id, v.version_number, v.state, v.tree::jsonb, v.published_at, now(), now()
FROM (VALUES
    ('Login failed: server error', 1, 'RETIRED', now() - interval '30 days', '{
      "start": "app-version",
      "steps": {
        "app-version": {
          "question": "Is the app on version 5.0 or later?",
          "answers": [
            { "key": "yes", "label": "Yes", "outcome": "escalate-sre" },
            { "key": "no",  "label": "No",  "outcome": "update-app" }
          ]
        }
      },
      "outcomes": {
        "update-app":   { "type": "RESOLVED", "result": "The app was updated and login works.",
                          "customerScript": "Please update the app from the store, then log in again." },
        "escalate-sre": { "type": "ESCALATE", "result": "Login fails on a current app.",
                          "customerScript": "I have passed this to our engineers and will call you back with an update." }
      }
    }'),
    ('Login failed: server error', 2, 'LIVE', now() - interval '7 days', '{
      "start": "app-version",
      "steps": {
        "app-version": {
          "question": "Is the app on version 5.2 or later?",
          "answers": [
            { "key": "yes", "label": "Yes", "next": "other-device" },
            { "key": "no",  "label": "No",  "outcome": "update-app" }
          ]
        },
        "other-device": {
          "question": "Can the customer log in on the web or on another device?",
          "answers": [
            { "key": "yes", "label": "Yes", "outcome": "clear-app-data" },
            { "key": "no",  "label": "No",  "next": "status-page" }
          ],
          "hints": [
            { "errorCode": "ERR_AUTH_503", "answerKey": "no",
              "text": "ERR_AUTH_503 means the login service did not answer, so it usually fails on every device." }
          ]
        },
        "status-page": {
          "question": "Does the status page show a login incident right now?",
          "answers": [
            { "key": "yes", "label": "Yes", "outcome": "wait-for-incident" },
            { "key": "no",  "label": "No",  "outcome": "escalate-sre" }
          ]
        }
      },
      "outcomes": {
        "update-app":        { "type": "RESOLVED", "result": "The app was updated and login works.",
                               "customerScript": "Please update the app from the store, then log in again." },
        "clear-app-data":    { "type": "RESOLVED", "result": "Clearing the app data fixed login on this device.",
                               "customerScript": "Please clear the app data in your phone settings and log in again." },
        "wait-for-incident": { "type": "FOLLOW_UP", "result": "Login is down for everyone; waiting for the fix.",
                               "customerScript": "We know about this problem and are fixing it. We will call you when it works again." },
        "escalate-sre":      { "type": "ESCALATE", "result": "Login fails everywhere with no known incident.",
                               "customerScript": "I have passed this to our engineers and will call you back with an update." }
      }
    }'),
    ('Login failed: server error', 3, 'DRAFT', NULL, '{
      "start": "app-version",
      "steps": {
        "app-version": {
          "question": "Draft: is the app on version 6.0 or later?",
          "answers": [
            { "key": "yes", "label": "Yes", "outcome": "escalate-sre" },
            { "key": "no",  "label": "No",  "outcome": "escalate-sre" }
          ]
        }
      },
      "outcomes": {
        "escalate-sre": { "type": "ESCALATE", "result": "Draft outcome.", "customerScript": "Draft script." }
      }
    }'),
    ('Login failed: general checks', 1, 'LIVE', now() - interval '14 days', '{
      "start": "password-reset",
      "steps": {
        "password-reset": {
          "question": "Has the customer reset the password in the last 24 hours?",
          "answers": [
            { "key": "yes", "label": "Yes", "next": "locked-message" },
            { "key": "no",  "label": "No",  "outcome": "reset-password" }
          ]
        },
        "locked-message": {
          "question": "Does the app say the account is locked?",
          "answers": [
            { "key": "yes", "label": "Yes", "outcome": "wait-for-unlock" },
            { "key": "no",  "label": "No",  "outcome": "escalate-sre" }
          ]
        }
      },
      "outcomes": {
        "reset-password":  { "type": "RESOLVED", "result": "A password reset fixed login.",
                             "customerScript": "Please use Forgot password in the app to set a new password." },
        "wait-for-unlock": { "type": "FOLLOW_UP", "result": "The account is locked for 15 minutes after failed logins.",
                             "customerScript": "Your account unlocks by itself in 15 minutes. We will call you to check it works." },
        "escalate-sre":    { "type": "ESCALATE", "result": "Login fails after a reset with no lock.",
                             "customerScript": "I have passed this to our engineers and will call you back with an update." }
      }
    }'),
    ('Payment failed', 1, 'LIVE', now() - interval '10 days', '{
      "start": "debited",
      "steps": {
        "debited": {
          "question": "Was the customer''s account debited for the payment?",
          "answers": [
            { "key": "yes", "label": "Yes", "outcome": "escalate-debit" },
            { "key": "no",  "label": "No",  "next": "retry" }
          ],
          "hints": [
            { "errorCode": "PAY_TIMEOUT", "answerKey": "no",
              "text": "PAY_TIMEOUT usually means the payment never reached the merchant, so nothing was debited." }
          ]
        },
        "retry": {
          "question": "Did a retry after 10 minutes go through?",
          "answers": [
            { "key": "yes", "label": "Yes", "outcome": "retried" },
            { "key": "no",  "label": "No",  "outcome": "escalate-debit" }
          ]
        }
      },
      "outcomes": {
        "retried":        { "type": "RESOLVED", "result": "The payment went through on retry.",
                            "customerScript": "Your payment has now gone through. Nothing else is needed." },
        "escalate-debit": { "type": "ESCALATE", "result": "The payment failed and needs an engineer.",
                            "customerScript": "I have passed this to our payments team and will call you back with an update." }
      }
    }')
) AS v (title, version_number, state, published_at, tree)
JOIN guides g ON g.title = v.title
ON CONFLICT (guide_id, version_number) DO NOTHING;   -- published trees are never rewritten, runs are pinned to them
