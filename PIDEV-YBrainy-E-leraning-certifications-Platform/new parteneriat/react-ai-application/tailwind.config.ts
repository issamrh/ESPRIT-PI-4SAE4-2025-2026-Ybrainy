import type { Config } from 'tailwindcss';

const config: Config = {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        brand: {
          primary: '#4D44B5',
          secondary: '#FB7D5B',
          deep: '#1E1A46',
          ink: '#49528D',
          mist: '#F2F5FF',
        },
      },
      fontFamily: {
        display: ['Poppins', 'sans-serif'],
        body: ['Nunito Sans', 'sans-serif'],
      },
      boxShadow: {
        card: '0 20px 45px rgba(30, 26, 70, 0.15)',
        glow: '0 0 0 6px rgba(77, 68, 181, 0.14), 0 16px 35px rgba(77, 68, 181, 0.25)',
      },
      keyframes: {
        float: {
          '0%, 100%': { transform: 'translateY(0px)' },
          '50%': { transform: 'translateY(-6px)' },
        },
      },
      animation: {
        float: 'float 3s ease-in-out infinite',
      },
    },
  },
  plugins: [],
};

export default config;
