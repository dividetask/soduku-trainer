# Claude Working Notes

## Interaction style

- **Do not use the AskUserQuestion tool / question prompt.** When clarification is needed, ask the question in plain text and STOP execution — wait for the user to reply before continuing.
- After asking a clarifying question, end the turn. Do not proceed with any tool calls or implementation until the user responds.
