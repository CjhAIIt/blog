(function () {
    const templates = {
        clean: {
            backgroundColor: '#f8fafc',
            themeColor: '#2563eb',
            fontFamily: 'Inter, system-ui, sans-serif',
            backgroundImage: ''
        },
        ink: {
            backgroundColor: '#111827',
            themeColor: '#8b5cf6',
            fontFamily: "'SFMono-Regular', Consolas, 'Liberation Mono', monospace",
            backgroundImage: '/images/default-covers/cover-abstract-shadow.jpg'
        },
        garden: {
            backgroundColor: '#f4f7f2',
            themeColor: '#477a5b',
            fontFamily: "Georgia, 'Times New Roman', serif",
            backgroundImage: '/images/default-covers/cover-library-light.jpg'
        },
        sunset: {
            backgroundColor: '#fff4ea',
            themeColor: '#c2410c',
            fontFamily: "'Trebuchet MS', 'Arial Rounded MT Bold', system-ui, sans-serif",
            backgroundImage: '/images/default-covers/cover-writing-desk.jpg'
        },
        ocean: {
            backgroundColor: '#eef8fb',
            themeColor: '#0f766e',
            fontFamily: 'Inter, system-ui, sans-serif',
            backgroundImage: '/images/default-covers/cover-mountain-view.jpg'
        }
    };

    const fontOptions = {
        system: 'Inter, system-ui, sans-serif',
        serif: "Georgia, 'Times New Roman', serif",
        mono: "'SFMono-Regular', Consolas, 'Liberation Mono', monospace",
        rounded: "'Trebuchet MS', 'Arial Rounded MT Bold', system-ui, sans-serif"
    };

    function clone(value) {
        return JSON.parse(JSON.stringify(value || {}));
    }

    function csrfHeaders() {
        const token = document.querySelector('meta[name="_csrf"]')?.content;
        const header = document.querySelector('meta[name="_csrf_header"]')?.content;
        return token && header ? { [header]: token } : {};
    }

    function normalizeStyle(style) {
        return {
            backgroundImage: style.backgroundImage || '',
            backgroundColor: style.backgroundColor || '#f8fafc',
            themeColor: style.themeColor || '#2563eb',
            fontFamily: style.fontFamily || fontOptions.system,
            profile: style.profile || '',
            signature: style.signature || '',
            tags: Array.isArray(style.tags) ? style.tags : [],
            templateId: style.templateId || 'clean'
        };
    }

    function initBackgroundEditor(root, state, patch) {
        root.querySelector('[data-field="backgroundColor"]')?.addEventListener('input', (event) => {
            patch({ backgroundColor: event.target.value });
        });
        root.querySelector('[data-field="themeColor"]')?.addEventListener('input', (event) => {
            patch({ themeColor: event.target.value });
        });
        root.querySelector('[data-action="clearBackground"]')?.addEventListener('click', () => {
            patch({ backgroundImage: '' });
        });
        root.querySelector('[data-field="backgroundFile"]')?.addEventListener('change', async (event) => {
            const file = event.target.files?.[0];
            if (!file) {
                return;
            }
            const localUrl = URL.createObjectURL(file);
            patch({ backgroundImage: localUrl });

            const formData = new FormData();
            formData.append('file', file);
            const response = await fetch('/api/me/space-style/background', {
                method: 'POST',
                headers: csrfHeaders(),
                body: formData
            });
            if (!response.ok) {
                throw new Error('背景上传失败');
            }
            const data = await response.json();
            patch({ backgroundImage: data.url || localUrl });
        });
    }

    function initFontSelector(root, patch) {
        root.querySelector('[data-field="fontFamily"]')?.addEventListener('change', (event) => {
            patch({ fontFamily: event.target.value });
        });
    }

    function initTemplateSelector(root, patch) {
        root.querySelectorAll('[data-template]').forEach((button) => {
            button.addEventListener('click', () => {
                const templateId = button.dataset.template;
                patch({ ...templates[templateId], templateId });
            });
        });
    }

    function initProfileEditor(root, getState, patch) {
        root.querySelector('[data-field="profile"]')?.addEventListener('input', (event) => {
            patch({ profile: event.target.value });
        });
        root.querySelector('[data-field="signature"]')?.addEventListener('input', (event) => {
            patch({ signature: event.target.value });
        });
        const tagInput = root.querySelector('[data-field="tagInput"]');
        tagInput?.addEventListener('keydown', (event) => {
            if (event.key !== 'Enter') {
                return;
            }
            event.preventDefault();
            const value = tagInput.value.trim();
            if (!value) {
                return;
            }
            const tags = Array.from(new Set([...(getState().tags || []), value])).slice(0, 10);
            tagInput.value = '';
            patch({ tags });
        });
        root.addEventListener('click', (event) => {
            const removeButton = event.target.closest('[data-remove-tag]');
            if (!removeButton) {
                return;
            }
            const tag = removeButton.dataset.removeTag;
            patch({ tags: getState().tags.filter((item) => item !== tag) });
        });
    }

    function initSpaceStyleEditor(root) {
        let saved = normalizeStyle(JSON.parse(root.dataset.initialStyle || '{}'));
        let draft = clone(saved);
        let versions = JSON.parse(root.dataset.versions || '[]');

        const status = root.querySelector('[data-role="status"]');
        const preview = root.querySelector('[data-role="preview"]');
        const previewProfile = root.querySelector('[data-preview="profile"]');
        const previewSignature = root.querySelector('[data-preview="signature"]');
        const previewTags = root.querySelector('[data-preview="tags"]');
        const tagList = root.querySelector('[data-role="tagList"]');
        const versionList = root.querySelector('[data-role="versionList"]');

        function setStatus(message, type) {
            if (!status) {
                return;
            }
            status.textContent = message || '';
            status.dataset.type = type || 'idle';
        }

        function patch(payload) {
            draft = normalizeStyle({ ...draft, ...payload });
            render();
        }

        function render() {
            root.querySelector('[data-field="backgroundColor"]').value = draft.backgroundColor;
            root.querySelector('[data-field="themeColor"]').value = draft.themeColor;
            root.querySelector('[data-field="fontFamily"]').value = draft.fontFamily;
            root.querySelector('[data-field="profile"]').value = draft.profile;
            root.querySelector('[data-field="signature"]').value = draft.signature;

            preview.style.setProperty('--space-theme', draft.themeColor);
            preview.style.setProperty('--space-bg', draft.backgroundColor);
            preview.style.setProperty('--space-font', draft.fontFamily);
            preview.style.backgroundColor = draft.backgroundColor;
            preview.style.fontFamily = draft.fontFamily;
            preview.style.backgroundImage = draft.backgroundImage
                ? `linear-gradient(120deg, rgba(255,255,255,.82), rgba(255,255,255,.58)), url("${draft.backgroundImage}")`
                : '';

            previewProfile.textContent = draft.profile || '还没有写个人简介，先用几句话介绍一下自己。';
            previewSignature.textContent = draft.signature || '在这里写一句签名。';
            previewTags.innerHTML = '';
            tagList.innerHTML = '';
            (draft.tags || []).forEach((tag) => {
                const previewTag = document.createElement('span');
                previewTag.textContent = tag;
                previewTags.appendChild(previewTag);

                const item = document.createElement('button');
                item.type = 'button';
                item.className = 'll-style-tag';
                item.dataset.removeTag = tag;
                item.textContent = `${tag} x`;
                tagList.appendChild(item);
            });

            root.querySelectorAll('[data-template]').forEach((button) => {
                button.classList.toggle('is-active', button.dataset.template === draft.templateId);
            });
            renderVersions();
        }

        function renderVersions() {
            if (!versionList) {
                return;
            }
            versionList.innerHTML = '';
            if (!versions.length) {
                const empty = document.createElement('div');
                empty.className = 'll-style-empty';
                empty.textContent = '保存一次风格后，这里会出现可恢复的历史版本。';
                versionList.appendChild(empty);
                return;
            }
            versions.forEach((version) => {
                const item = document.createElement('button');
                item.type = 'button';
                item.className = 'll-style-version';
                item.dataset.restoreVersion = version.id;
                item.innerHTML = `<strong>${version.versionName || '历史版本'}</strong><span>${String(version.createdAt || '').replace('T', ' ').slice(0, 16)}</span>`;
                versionList.appendChild(item);
            });
        }

        async function refreshVersions() {
            const response = await fetch('/api/me/space-style/versions', { headers: csrfHeaders() });
            if (response.ok) {
                versions = await response.json();
            }
        }

        root.querySelector('[data-action="save"]')?.addEventListener('click', async () => {
            setStatus('正在保存...', 'loading');
            const response = await fetch('/api/me/space-style', {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json', ...csrfHeaders() },
                body: JSON.stringify(draft)
            });
            if (!response.ok) {
                setStatus('保存失败，请稍后重试。', 'error');
                return;
            }
            saved = normalizeStyle(await response.json());
            draft = clone(saved);
            await refreshVersions();
            render();
            setStatus('已保存，个人空间会自动加载这套风格。', 'success');
        });

        root.querySelector('[data-action="cancel"]')?.addEventListener('click', () => {
            draft = clone(saved);
            render();
            setStatus('已取消未保存的修改。', 'idle');
        });

        root.querySelector('[data-action="reset"]')?.addEventListener('click', async () => {
            setStatus('正在恢复默认...', 'loading');
            const response = await fetch('/api/me/space-style/reset', {
                method: 'POST',
                headers: csrfHeaders()
            });
            if (!response.ok) {
                setStatus('恢复默认失败。', 'error');
                return;
            }
            saved = normalizeStyle(await response.json());
            draft = clone(saved);
            await refreshVersions();
            render();
            setStatus('已恢复默认风格。', 'success');
        });

        root.addEventListener('click', async (event) => {
            const button = event.target.closest('[data-restore-version]');
            if (!button) {
                return;
            }
            setStatus('正在恢复历史版本...', 'loading');
            const response = await fetch(`/api/me/space-style/versions/${button.dataset.restoreVersion}/restore`, {
                method: 'POST',
                headers: csrfHeaders()
            });
            if (!response.ok) {
                setStatus('恢复历史版本失败。', 'error');
                return;
            }
            saved = normalizeStyle(await response.json());
            draft = clone(saved);
            await refreshVersions();
            render();
            setStatus('已恢复历史版本。', 'success');
        });

        initBackgroundEditor(root, draft, patch);
        initFontSelector(root, patch);
        initTemplateSelector(root, patch);
        initProfileEditor(root, () => draft, patch);
        render();
    }

    window.SpaceStyleEditor = { init: initSpaceStyleEditor };
})();
