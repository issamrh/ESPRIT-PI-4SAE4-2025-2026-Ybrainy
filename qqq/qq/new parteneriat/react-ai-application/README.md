# AI Application Generator (React + Tailwind)

Modern UI module for CV optimization + cover letter generation.

## Features

- Drag-and-drop CV upload
- Job description input card
- Animated generate/regenerate button
- Loader + smooth transitions (Framer Motion)
- Result tabs (Optimized CV / Cover Letter)
- Editable result cards
- Copy to clipboard
- Download PDF
- ATS keyword score insight

## Run locally

```bash
npm install
npm run dev
```

The app runs on `http://localhost:5173`.

## Environment

Create `.env` from `.env.example`:

```bash
cp .env.example .env
```

`VITE_API_BASE_URL` should point to your gateway:

- `http://localhost:8080`

## Backend endpoint used

`POST /api/generate-application`
