(function () {
    function getThemePrefix(form) {
        return form.closest('.mb-main') || form.className.includes('mb-') ? 'mb' : 'll';
    }

    function getAssociatedFields(form) {
        return Array.from(form.querySelectorAll('input, select, textarea'))
            .filter(field => field.form === form && field.type !== 'hidden' && !field.disabled);
    }

    function getSubmitButtons(form) {
        return Array.from(form.querySelectorAll('button[type="submit"], input[type="submit"]'))
            .filter(button => button.form === form);
    }

    function getRequiredFields(form) {
        return getAssociatedFields(form).filter(field => field.required);
    }

    function getProgressElement(form, prefix) {
        const existing = form.querySelector('[data-form-progress]');
        if (existing) {
            return existing;
        }

        const progress = document.createElement('div');
        progress.className = prefix + '-form-progress';
        progress.dataset.formProgress = 'true';
        progress.setAttribute('role', 'status');
        progress.setAttribute('aria-live', 'polite');

        const actionGroups = Array.from(form.querySelectorAll('.' + prefix + '-form-actions'))
            .filter(group => group.closest('form') === form);
        const anchor = actionGroups[actionGroups.length - 1];
        if (anchor?.parentNode) {
            anchor.parentNode.insertBefore(progress, anchor);
        } else {
            form.appendChild(progress);
        }
        return progress;
    }

    function getFieldLabel(form, field) {
        if (field.id) {
            const explicit = form.querySelector('label[for="' + field.id + '"]');
            if (explicit?.textContent) {
                return explicit.textContent.replace(/\s+/g, ' ').trim();
            }
        }

        const nearbyLabel = field.closest('.ll-form-field, .mb-field')?.querySelector('label');
        if (nearbyLabel?.textContent) {
            return nearbyLabel.textContent.replace(/\s+/g, ' ').trim();
        }

        return field.getAttribute('aria-label') || field.getAttribute('placeholder') || field.name || '这项';
    }

    function getFieldContainer(field) {
        return field.closest('.ll-form-field, .mb-field') || field.parentElement;
    }

    function normalizeFieldInput(field, prefix) {
        if (typeof field.value !== 'string') {
            return;
        }

        const shouldTrim = field.dataset.trimInput === 'true' || field.dataset.normalizeUrl === 'true';
        let nextValue = shouldTrim ? field.value.trim() : field.value;
        let helperText = '';
        let helperTone = '';

        if (field.dataset.normalizeUrl === 'true' && nextValue) {
            const hasScheme = /^[a-z][a-z0-9+.-]*:/i.test(nextValue);
            const isRelative = nextValue.startsWith('/') || nextValue.startsWith('#');
            if (!hasScheme && !isRelative) {
                nextValue = 'https://' + nextValue.replace(/^\/+/, '');
                helperText = field.dataset.normalizeUrlMessage || '已自动补全为 https:// 开头。';
                helperTone = 'good';
            }
        }

        if (nextValue !== field.value) {
            field.value = nextValue;
        }

        if (field.dataset.normalizeUrl === 'true') {
            setFieldMessage(field, prefix, 'helper', helperText, helperTone);
        }
    }

    function enhancePasswordToggle(field, prefix) {
        if (field.dataset.passwordToggleReady === 'true') {
            return;
        }

        const wrapper = document.createElement('div');
        wrapper.className = prefix + '-input-shell';
        field.parentNode.insertBefore(wrapper, field);
        wrapper.appendChild(field);

        const button = document.createElement('button');
        button.type = 'button';
        button.className = prefix + '-input-action';
        button.textContent = '显示';
        button.setAttribute('aria-label', '显示密码');
        wrapper.appendChild(button);

        button.addEventListener('click', () => {
            const showing = field.type === 'text';
            field.type = showing ? 'password' : 'text';
            button.textContent = showing ? '显示' : '隐藏';
            button.setAttribute('aria-label', showing ? '显示密码' : '隐藏密码');
            field.focus({preventScroll: true});
        });

        field.dataset.passwordToggleReady = 'true';
    }

    function getFieldMessageElement(field, prefix, type) {
        const container = getFieldContainer(field);
        if (!container) {
            return null;
        }

        const selector = '[data-field-' + type + '="' + field.name + '"]';
        let element = container.querySelector(selector);
        if (element) {
            return element;
        }

        element = document.createElement('div');
        element.className = prefix + '-field-' + type;
        element.dataset['field' + type.charAt(0).toUpperCase() + type.slice(1)] = field.name;
        container.appendChild(element);
        return element;
    }

    function setFieldMessage(field, prefix, type, text, tone) {
        const element = getFieldMessageElement(field, prefix, type);
        if (!element) {
            return;
        }

        if (!text) {
            element.hidden = true;
            element.textContent = '';
            element.removeAttribute('data-tone');
            return;
        }

        element.hidden = false;
        element.textContent = text;
        if (tone) {
            element.dataset.tone = tone;
        } else {
            element.removeAttribute('data-tone');
        }
    }

    function setFieldValidityState(field) {
        if (field.validity.valid) {
            field.removeAttribute('aria-invalid');
        } else {
            field.setAttribute('aria-invalid', 'true');
        }
    }

    function setProgress(progress, tone, text) {
        if (!progress) {
            return;
        }
        progress.dataset.tone = tone;
        progress.textContent = text;
    }

    function getFieldMessage(form, field) {
        const label = getFieldLabel(form, field);

        if (field.validity.customError) {
            return field.validationMessage;
        }

        if (field.validity.valueMissing) {
            return field.dataset.valueMissingMessage || (label + '还没填。');
        }

        if (field.validity.typeMismatch) {
            if (field.type === 'email') {
                return field.dataset.typeMismatchMessage || '邮箱格式像 name@example.com 这样就可以。';
            }
            if (field.type === 'url') {
                return field.dataset.typeMismatchMessage || '链接格式看起来还不完整。';
            }
        }

        if (field.validity.patternMismatch) {
            return field.dataset.patternMismatchMessage || (label + '格式还不对。');
        }

        if (field.validity.tooShort) {
            return field.dataset.tooShortMessage || (label + '至少 ' + field.minLength + ' 位。');
        }

        if (field.validity.tooLong) {
            return field.dataset.tooLongMessage || (label + '已经超出可填写长度。');
        }

        return field.validationMessage || (label + '还需要再检查一下。');
    }

    function updateFieldError(form, field, prefix) {
        const shouldShow = field.dataset.fieldTouched === 'true' || form.dataset.formSubmitting === 'true';
        if (!shouldShow || field.validity.valid) {
            setFieldMessage(field, prefix, 'error', '');
            return;
        }

        setFieldMessage(field, prefix, 'error', getFieldMessage(form, field));
    }

    function getMissingFields(requiredFields) {
        return requiredFields.filter(field => !field.checkValidity());
    }

    function buildMissingText(form, missingFields) {
        const missingLabels = missingFields
            .slice(0, 3)
            .map(field => getFieldLabel(form, field));
        const extraCount = missingFields.length - missingLabels.length;
        const labelText = missingLabels.join('、') + (extraCount > 0 ? ' 等 ' + missingFields.length + ' 项' : '');
        return '还差 ' + missingFields.length + ' 项：' + labelText + '。';
    }

    function buildInvalidText(form, invalidFields) {
        const invalidLabels = invalidFields
            .slice(0, 3)
            .map(field => getFieldLabel(form, field));
        const extraCount = invalidFields.length - invalidLabels.length;
        const labelText = invalidLabels.join('、') + (extraCount > 0 ? ' 等 ' + invalidFields.length + ' 项' : '');
        return '还需要再检查 ' + invalidFields.length + ' 项：' + labelText + '。';
    }

    function updateFormProgress(form, progress) {
        if (form.dataset.formSubmitting === 'true') {
            return;
        }

        const requiredFields = getRequiredFields(form);
        const missingFields = getMissingFields(requiredFields);
        if (missingFields.length) {
            if (form.dataset.formTouched === 'true') {
                setProgress(progress, 'warn', buildMissingText(form, missingFields));
                return;
            }

            setProgress(
                progress,
                'neutral',
                form.dataset.pendingText || ('先把 ' + missingFields.length + ' 项必填信息填好，再提交也不迟。')
            );
            return;
        }

        const invalidFields = getAssociatedFields(form).filter(field => !field.validity.valid);
        if (invalidFields.length) {
            if (form.dataset.formTouched === 'true') {
                setProgress(progress, 'warn', buildInvalidText(form, invalidFields));
                return;
            }

            setProgress(progress, 'neutral', '还有几项细节没对上，慢慢改完再提交就行。');
            return;
        }

        setProgress(
            progress,
            requiredFields.length ? 'good' : 'neutral',
            form.dataset.readyText || (requiredFields.length ? '必填项已经填好，可以直接提交。' : '内容已经整理好了，提交时会自动锁定按钮，避免重复操作。')
        );
    }

    function describePasswordStrength(value, minLength) {
        if (!value) {
            return {tone: 'neutral', text: '建议至少 ' + minLength + ' 位，别太简单就行。'};
        }

        if (value.length < minLength) {
            return {tone: 'warn', text: '再补 ' + (minLength - value.length) + ' 位会更稳。'};
        }

        const variety = [
            /[a-z]/.test(value),
            /[A-Z]/.test(value),
            /\d/.test(value),
            /[^A-Za-z0-9]/.test(value)
        ].filter(Boolean).length;

        if (variety >= 3) {
            return {tone: 'good', text: '长度和组合都够用了。'};
        }

        if (variety === 2) {
            return {tone: 'neutral', text: '长度够了，再混一点不同字符会更稳。'};
        }

        return {tone: 'warn', text: '长度到了，但还是建议混一些字母、数字或符号。'};
    }

    function syncPasswordRules(form, prefix) {
        const primary = form.querySelector('[data-password-primary]');
        const confirm = form.querySelector('[data-password-confirm]');
        const current = form.querySelector('[data-password-current]');

        if (!primary && !confirm && !current) {
            return;
        }

        const minLength = Math.max(Number(primary?.getAttribute('minlength')) || 0, 8);
        const primaryValue = primary?.value || '';
        const confirmValue = confirm?.value || '';
        const currentValue = current?.value || '';

        if (primary) {
            const strength = describePasswordStrength(primaryValue, minLength);
            setFieldMessage(primary, prefix, 'helper', strength.text, strength.tone);
        }

        if (confirm) {
            let confirmText = '再输入一次，避免打错。';
            let confirmTone = 'neutral';
            confirm.setCustomValidity('');

            if (!primaryValue) {
                if (confirmValue) {
                    confirm.setCustomValidity('先输入上面的密码。');
                    confirmText = '先把上面的密码写好，再确认一次。';
                    confirmTone = 'warn';
                }
            } else if (!confirmValue) {
                confirmText = '再输入一次，确认没有打错。';
                confirmTone = 'neutral';
            } else if (confirmValue !== primaryValue) {
                confirm.setCustomValidity('两次输入的密码还不一致。');
                confirmText = '两次输入还不一致，再对一遍。';
                confirmTone = 'warn';
            } else {
                confirmText = '两次输入已经一致。';
                confirmTone = 'good';
            }

            setFieldMessage(confirm, prefix, 'helper', confirmText, confirmTone);
        }

        if (current) {
            let currentText = '只有改密码时才需要填当前密码。';
            let currentTone = 'neutral';
            current.setCustomValidity('');

            if (primaryValue) {
                if (!currentValue) {
                    current.setCustomValidity('修改密码前，请先输入当前密码。');
                    currentText = '改密码前，需要先填当前密码。';
                    currentTone = 'warn';
                } else {
                    currentText = '当前密码已填写，保存时会按新密码更新。';
                    currentTone = 'good';
                }
            }

            setFieldMessage(current, prefix, 'helper', currentText, currentTone);
        }
    }

    function setSubmitterBusyText(submitter, text) {
        if (!submitter || !text) {
            return;
        }

        if (submitter.tagName === 'INPUT') {
            if (!submitter.dataset.originalValue) {
                submitter.dataset.originalValue = submitter.value;
            }
            submitter.value = text;
            return;
        }

        if (!submitter.dataset.originalText) {
            submitter.dataset.originalText = submitter.textContent;
        }
        submitter.textContent = text;
    }

    document.querySelectorAll('form[data-enhanced-form]').forEach(form => {
        const prefix = getThemePrefix(form);
        const progress = getProgressElement(form, prefix);
        const fields = getAssociatedFields(form);
        const submitButtons = getSubmitButtons(form);
        let lastSubmitter = null;

        fields
            .filter(field => field.dataset.passwordToggle === 'true')
            .forEach(field => enhancePasswordToggle(field, prefix));

        fields.forEach(field => {
            ['input', 'change', 'blur'].forEach(eventName => {
                field.addEventListener(eventName, () => {
                    form.dataset.formTouched = 'true';
                    field.dataset.fieldTouched = 'true';
                    if (eventName === 'blur') {
                        normalizeFieldInput(field, prefix);
                    }
                    syncPasswordRules(form, prefix);
                    setFieldValidityState(field);
                    updateFieldError(form, field, prefix);
                    updateFormProgress(form, progress);
                });
            });
        });

        submitButtons.forEach(button => {
            button.addEventListener('click', () => {
                lastSubmitter = button;
            });
        });

        form.addEventListener('invalid', event => {
            if (!(event.target instanceof HTMLElement)) {
                return;
            }
            form.dataset.formTouched = 'true';
            event.target.dataset.fieldTouched = 'true';
            syncPasswordRules(form, prefix);
            setFieldValidityState(event.target);
            updateFieldError(form, event.target, prefix);
            const requiredFields = getAssociatedFields(form).filter(field => !field.disabled && !field.validity.valid);
            const invalidCount = requiredFields.length;
            requiredFields.forEach(field => updateFieldError(form, field, prefix));
            const text = invalidCount > 0
                ? '还有 ' + invalidCount + ' 项没填好，先看标红的输入框。'
                : '还有内容没填好，先看标红的输入框。';
            setProgress(progress, 'warn', text);
        }, true);

        form.addEventListener('submit', event => {
            fields.forEach(field => normalizeFieldInput(field, prefix));
            syncPasswordRules(form, prefix);
            fields.forEach(field => {
                field.dataset.fieldTouched = 'true';
                setFieldValidityState(field);
                updateFieldError(form, field, prefix);
            });
            if (!form.checkValidity()) {
                return;
            }

            const submitter = event.submitter || lastSubmitter || submitButtons[0] || null;
            const submittingText = submitter?.dataset.submittingText || form.dataset.submittingText || '正在提交...';

            form.dataset.formSubmitting = 'true';
            form.setAttribute('aria-busy', 'true');
            setProgress(progress, 'busy', submittingText);
            setSubmitterBusyText(submitter, submittingText);
            submitButtons.forEach(button => {
                button.disabled = true;
            });
        });

        syncPasswordRules(form, prefix);
        fields.forEach(field => {
            setFieldValidityState(field);
            updateFieldError(form, field, prefix);
        });
        updateFormProgress(form, progress);
    });
}());
