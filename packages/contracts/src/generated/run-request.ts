// Generated from run-request.schema.json by scripts/generate.mjs. Do not edit.

/**
 * Body of POST /api/v1/runs, sent by the web to the api.
 */
export interface RunRequest {
  challengeId: string;
  /**
   * The editable files of the challenge, as the learner left them.
   *
   * @maxItems 10
   */
  files:
    | []
    | [
        {
          path: string;
          content: string;
        }
      ]
    | [
        {
          path: string;
          content: string;
        },
        {
          path: string;
          content: string;
        }
      ]
    | [
        {
          path: string;
          content: string;
        },
        {
          path: string;
          content: string;
        },
        {
          path: string;
          content: string;
        }
      ]
    | [
        {
          path: string;
          content: string;
        },
        {
          path: string;
          content: string;
        },
        {
          path: string;
          content: string;
        },
        {
          path: string;
          content: string;
        }
      ]
    | [
        {
          path: string;
          content: string;
        },
        {
          path: string;
          content: string;
        },
        {
          path: string;
          content: string;
        },
        {
          path: string;
          content: string;
        },
        {
          path: string;
          content: string;
        }
      ]
    | [
        {
          path: string;
          content: string;
        },
        {
          path: string;
          content: string;
        },
        {
          path: string;
          content: string;
        },
        {
          path: string;
          content: string;
        },
        {
          path: string;
          content: string;
        },
        {
          path: string;
          content: string;
        }
      ]
    | [
        {
          path: string;
          content: string;
        },
        {
          path: string;
          content: string;
        },
        {
          path: string;
          content: string;
        },
        {
          path: string;
          content: string;
        },
        {
          path: string;
          content: string;
        },
        {
          path: string;
          content: string;
        },
        {
          path: string;
          content: string;
        }
      ]
    | [
        {
          path: string;
          content: string;
        },
        {
          path: string;
          content: string;
        },
        {
          path: string;
          content: string;
        },
        {
          path: string;
          content: string;
        },
        {
          path: string;
          content: string;
        },
        {
          path: string;
          content: string;
        },
        {
          path: string;
          content: string;
        },
        {
          path: string;
          content: string;
        }
      ]
    | [
        {
          path: string;
          content: string;
        },
        {
          path: string;
          content: string;
        },
        {
          path: string;
          content: string;
        },
        {
          path: string;
          content: string;
        },
        {
          path: string;
          content: string;
        },
        {
          path: string;
          content: string;
        },
        {
          path: string;
          content: string;
        },
        {
          path: string;
          content: string;
        },
        {
          path: string;
          content: string;
        }
      ]
    | [
        {
          path: string;
          content: string;
        },
        {
          path: string;
          content: string;
        },
        {
          path: string;
          content: string;
        },
        {
          path: string;
          content: string;
        },
        {
          path: string;
          content: string;
        },
        {
          path: string;
          content: string;
        },
        {
          path: string;
          content: string;
        },
        {
          path: string;
          content: string;
        },
        {
          path: string;
          content: string;
        },
        {
          path: string;
          content: string;
        }
      ];
}
