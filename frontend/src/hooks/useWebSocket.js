import { useEffect, useRef, useState, useCallback } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { WS_URL } from '../api';

const useWebSocket = (workbookId, sheetId, onCellUpdate) => {
    const [connected, setConnected] = useState(false);
    const [reconnecting, setReconnecting] = useState(false);
    const clientRef = useRef(null);
    const subscriptionRef = useRef(null);
    const onCellUpdateRef = useRef(onCellUpdate);

    // Keep the callback ref up to date without triggering reconnects
    useEffect(() => {
        onCellUpdateRef.current = onCellUpdate;
    }, [onCellUpdate]);

    useEffect(() => {
        if (!workbookId || !sheetId) return;

        const client = new Client({
            webSocketFactory: () => new SockJS(WS_URL),
            reconnectDelay: 3000,
            onConnect: () => {
                setConnected(true);
                setReconnecting(false);

                // Subscribe to sheet-level topic
                const topic = `/topic/workbook/${workbookId}/sheet/${sheetId}`;
                subscriptionRef.current = client.subscribe(topic, (message) => {
                    try {
                        const parsed = JSON.parse(message.body);
                        const cells = parsed.cells || parsed;
                        if (onCellUpdateRef.current) {
                            onCellUpdateRef.current(Array.isArray(cells) ? cells : [cells]);
                        }
                    } catch (e) {
                        console.error('Failed to parse WebSocket message', e);
                    }
                });
            },
            onDisconnect: () => {
                setConnected(false);
            },
            onStompError: (frame) => {
                console.error('STOMP error', frame);
                setConnected(false);
                setReconnecting(true);
            },
            onWebSocketClose: () => {
                setConnected(false);
                setReconnecting(true);
            },
        });

        clientRef.current = client;
        client.activate();

        return () => {
            if (subscriptionRef.current) {
                subscriptionRef.current.unsubscribe();
                subscriptionRef.current = null;
            }
            client.deactivate();
            setConnected(false);
            setReconnecting(false);
        };
    }, [workbookId, sheetId]);

    return { connected, reconnecting };
};

export default useWebSocket;
