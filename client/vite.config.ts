import path from 'path';
import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig(({ mode }) => {
    // Load env vars based on the current mode (development/production)
    const env = loadEnv(mode, '.', '');

    return {
        server: {
            port: 3000,
            host: '0.0.0.0',
        },
        plugins: [react()],
        resolve: {
            alias: {
                '@': path.resolve(__dirname, '.'),
            }
        },
        // Move the build config here
        build: {
            minify: 'terser',
            terserOptions: {
                compress: {
                    drop_console: true, // This removes console.logs
                    drop_debugger: true,
                },
            },
        },
    };
});