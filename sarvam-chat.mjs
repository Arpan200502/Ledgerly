import { SarvamAIClient } from "sarvamai";

const apiKey = process.env.SARVAM_API_KEY;
if (!apiKey) {
  throw new Error("SARVAM_API_KEY is missing from .env");
}

const client = new SarvamAIClient({
  apiSubscriptionKey: apiKey,
});

const response = await client.chat.completions({
  model: "sarvam-105b-conversations",
  messages: [
    {
      role: "user",
      content: "Say hello in one short sentence.",
    },
  ],
});

console.log(response);
